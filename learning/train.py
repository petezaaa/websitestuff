"""Train a curiosity-driven agent to play Minecraft on your GPU.

PPO for the policy + Random Network Distillation for intrinsic (curiosity)
reward. Single MineRL environment (they are heavy). Checkpoints and TensorBoard
logs are written so training resumes and you can watch it learn.

    python train.py --env MineRLBasaltFindCave-v0

Requires a working MineRL install and (ideally) a CUDA GPU. See README.md.
"""
from __future__ import annotations

import argparse
import os
import time

import numpy as np
import torch
import torch.nn as nn
from torch.utils.tensorboard import SummaryWriter

from config import Config
from env import make_env, NUM_ACTIONS
from model import ActorCritic
from rnd import RNDModel
from utils import RunningMeanStd


def parse_args() -> Config:
    cfg = Config()
    p = argparse.ArgumentParser()
    p.add_argument("--env", default=cfg.env_id)
    p.add_argument("--device", default=cfg.device)
    p.add_argument("--total-steps", type=int, default=cfg.total_steps)
    p.add_argument("--rollout-steps", type=int, default=cfg.rollout_steps)
    p.add_argument("--lr", type=float, default=cfg.learning_rate)
    p.add_argument("--seed", type=int, default=cfg.seed)
    p.add_argument("--no-resume", action="store_true")
    args = p.parse_args()
    cfg.env_id = args.env
    cfg.device = args.device
    cfg.total_steps = args.total_steps
    cfg.rollout_steps = args.rollout_steps
    cfg.learning_rate = args.lr
    cfg.seed = args.seed
    if args.no_resume:
        cfg.resume = False
    return cfg


def compute_gae(rewards, values, last_value, last_done, dones, gamma, lam, episodic):
    """Generalized Advantage Estimation for one reward stream."""
    n = len(rewards)
    adv = np.zeros(n, dtype=np.float32)
    last_gae = 0.0
    for t in reversed(range(n)):
        if t == n - 1:
            next_nonterminal = 1.0 - (last_done if episodic else 0.0)
            next_value = last_value
        else:
            next_nonterminal = 1.0 - (dones[t + 1] if episodic else 0.0)
            next_value = values[t + 1]
        delta = rewards[t] + gamma * next_value * next_nonterminal - values[t]
        last_gae = delta + gamma * lam * next_nonterminal * last_gae
        adv[t] = last_gae
    return adv, adv + values


def main():
    cfg = parse_args()
    np.random.seed(cfg.seed)
    torch.manual_seed(cfg.seed)

    device = torch.device("cuda" if (cfg.device == "cuda" and torch.cuda.is_available()) else "cpu")
    print(f"Device: {device}  ({torch.cuda.get_device_name(0) if device.type == 'cuda' else 'CPU only'})")

    env = make_env(cfg)
    C, H, W = cfg.frame_stack, cfg.frame_size, cfg.frame_size

    agent = ActorCritic(C, cfg.frame_size, NUM_ACTIONS).to(device)
    rnd = RNDModel(cfg.frame_size).to(device)
    opt = torch.optim.Adam(agent.parameters(), lr=cfg.learning_rate, eps=1e-5)
    rnd_opt = torch.optim.Adam(rnd.predictor.parameters(), lr=cfg.rnd_lr)

    obs_rms = RunningMeanStd(shape=(1, H, W))
    int_ret_rms = RunningMeanStd(shape=())
    writer = SummaryWriter(os.path.join(cfg.log_dir, f"{cfg.env_id}-{int(time.time())}"))

    os.makedirs(cfg.checkpoint_dir, exist_ok=True)
    ckpt_path = os.path.join(cfg.checkpoint_dir, "latest.pt")
    global_step, update = 0, 0
    if cfg.resume and os.path.isfile(ckpt_path):
        ck = torch.load(ckpt_path, map_location=device)
        agent.load_state_dict(ck["agent"])
        rnd.load_state_dict(ck["rnd"])
        opt.load_state_dict(ck["opt"])
        rnd_opt.load_state_dict(ck["rnd_opt"])
        obs_rms.__dict__.update(ck["obs_rms"])
        int_ret_rms.__dict__.update(ck["int_ret_rms"])
        global_step, update = ck["global_step"], ck["update"]
        print(f"Resumed from {ckpt_path} at step {global_step}.")

    T = cfg.rollout_steps
    obs_buf = np.zeros((T, C, H, W), dtype=np.uint8)
    nxt_single = np.zeros((T, 1, H, W), dtype=np.uint8)
    actions = np.zeros(T, dtype=np.int64)
    logprobs = np.zeros(T, dtype=np.float32)
    rew_ext = np.zeros(T, dtype=np.float32)
    dones = np.zeros(T, dtype=np.float32)
    val_ext = np.zeros(T, dtype=np.float32)
    val_int = np.zeros(T, dtype=np.float32)

    obs = env.reset()
    last_done = 0.0

    def policy_obs(np_batch):
        return torch.as_tensor(np_batch, dtype=torch.float32, device=device) / 255.0

    while global_step < cfg.total_steps:
        # ---- collect a rollout ------------------------------------------
        for t in range(T):
            obs_buf[t] = obs
            with torch.no_grad():
                a, lp, _, ve, vi = agent.get_action_and_value(policy_obs(obs[None]))
            action = int(a.item())
            next_obs, r, done, _info = env.step(action)

            actions[t] = action
            logprobs[t] = lp.item()
            val_ext[t] = ve.item()
            val_int[t] = vi.item()
            rew_ext[t] = r
            dones[t] = float(done)
            nxt_single[t, 0] = next_obs[-1]  # newest frame for curiosity

            global_step += 1
            last_done = float(done)
            obs = env.reset() if done else next_obs

        with torch.no_grad():
            _, _, _, last_ve, last_vi = agent.get_action_and_value(policy_obs(obs[None]))
        last_ve, last_vi = last_ve.item(), last_vi.item()

        # ---- intrinsic (curiosity) reward -------------------------------
        obs_rms.update(nxt_single.astype(np.float32).reshape(T, 1, H, W))
        norm = np.clip(
            (nxt_single.astype(np.float32) - obs_rms.mean) / (obs_rms.std + 1e-8),
            -cfg.obs_norm_clip, cfg.obs_norm_clip,
        ).astype(np.float32)
        with torch.no_grad():
            intr = rnd.intrinsic_reward(torch.as_tensor(norm, device=device)).cpu().numpy()
        int_ret_rms.update(intr.reshape(-1, 1))
        rew_int = intr / (float(int_ret_rms.std) + 1e-8)

        # ---- advantages (two streams) -----------------------------------
        adv_ext, ret_ext = compute_gae(rew_ext, val_ext, last_ve, last_done, dones,
                                       cfg.gamma_ext, cfg.gae_lambda, episodic=True)
        adv_int, ret_int = compute_gae(rew_int, val_int, last_vi, last_done, dones,
                                       cfg.gamma_int, cfg.gae_lambda, episodic=False)
        adv = cfg.ext_coef * adv_ext + cfg.int_coef * adv_int

        # ---- PPO + RND update -------------------------------------------
        b_obs = policy_obs(obs_buf)
        b_norm = torch.as_tensor(norm, device=device)
        b_act = torch.as_tensor(actions, device=device)
        b_logp = torch.as_tensor(logprobs, device=device)
        b_adv = torch.as_tensor(adv, device=device)
        b_ret_ext = torch.as_tensor(ret_ext, device=device)
        b_ret_int = torch.as_tensor(ret_int, device=device)

        inds = np.arange(T)
        last_stats = {}
        for _ in range(cfg.update_epochs):
            np.random.shuffle(inds)
            for start in range(0, T, cfg.minibatch_size):
                mb = inds[start:start + cfg.minibatch_size]
                _, new_lp, entropy, new_ve, new_vi = agent.get_action_and_value(b_obs[mb], b_act[mb])
                ratio = (new_lp - b_logp[mb]).exp()

                adv_mb = b_adv[mb]
                adv_mb = (adv_mb - adv_mb.mean()) / (adv_mb.std() + 1e-8)
                pg1 = -adv_mb * ratio
                pg2 = -adv_mb * torch.clamp(ratio, 1 - cfg.clip_coef, 1 + cfg.clip_coef)
                pg_loss = torch.max(pg1, pg2).mean()

                v_loss = 0.5 * ((new_ve - b_ret_ext[mb]) ** 2).mean() \
                    + 0.5 * ((new_vi - b_ret_int[mb]) ** 2).mean()
                ent = entropy.mean()
                loss = pg_loss + cfg.value_coef * v_loss - cfg.entropy_coef * ent

                # RND predictor learns on a random subset of the minibatch.
                keep = (torch.rand(len(mb), device=device) < cfg.rnd_update_proportion).float()
                rnd_loss = rnd.predictor_loss(b_norm[mb], keep_mask=keep)

                opt.zero_grad(set_to_none=True)
                rnd_opt.zero_grad(set_to_none=True)
                (loss + rnd_loss).backward()
                nn.utils.clip_grad_norm_(agent.parameters(), cfg.max_grad_norm)
                opt.step()
                rnd_opt.step()
                last_stats = dict(pg=pg_loss.item(), v=v_loss.item(),
                                  ent=ent.item(), rnd=rnd_loss.item())

        update += 1

        # ---- logging / checkpoint ---------------------------------------
        writer.add_scalar("reward/extrinsic_mean", float(rew_ext.mean()), global_step)
        writer.add_scalar("reward/intrinsic_mean", float(rew_int.mean()), global_step)
        writer.add_scalar("loss/policy", last_stats.get("pg", 0.0), global_step)
        writer.add_scalar("loss/value", last_stats.get("v", 0.0), global_step)
        writer.add_scalar("loss/entropy", last_stats.get("ent", 0.0), global_step)
        writer.add_scalar("loss/rnd", last_stats.get("rnd", 0.0), global_step)
        print(f"update {update} step {global_step}  "
              f"ext {rew_ext.mean():.3f}  int {rew_int.mean():.3f}  "
              f"ent {last_stats.get('ent', 0):.3f}")

        if update % cfg.checkpoint_every_updates == 0:
            torch.save({
                "agent": agent.state_dict(), "rnd": rnd.state_dict(),
                "opt": opt.state_dict(), "rnd_opt": rnd_opt.state_dict(),
                "obs_rms": obs_rms.__dict__, "int_ret_rms": int_ret_rms.__dict__,
                "global_step": global_step, "update": update, "config": cfg.to_dict(),
            }, ckpt_path)
            print(f"  saved checkpoint -> {ckpt_path}")

    env.close()
    writer.close()


if __name__ == "__main__":
    main()
