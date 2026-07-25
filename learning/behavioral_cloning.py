"""Behavioral cloning (BC) warm-start from MineRL human demonstrations.

Trains the policy to imitate human players — supervised learning of
(observation -> action) pairs — which gives the agent sensible behaviour to
start from before reinforcement learning + curiosity take over. This is far
faster than learning everything from scratch.

Usage:
    # one-time: download demo data for the env (a few GB)
    python -m minerl.data.download --environment MineRLTreechop-v0

    # then clone a policy from it
    python behavioral_cloning.py --env MineRLTreechop-v0 --epochs 2

    # then reinforce it
    python train.py --env MineRLTreechop-v0 --init-from checkpoints/bc_policy.pt

Note: this targets MineRL v0.4 style envs whose demo actions are a dict of
button flags + a `camera` delta (e.g. Treechop, ObtainDiamond, BASALT FindCave).
"""
from __future__ import annotations

import argparse
import os

import cv2
import numpy as np
import torch
import torch.nn as nn

from config import Config
from device import get_device
from env import discretize_action, NUM_ACTIONS
from model import ActorCritic


def to_gray(pov_bt, size):
    """(B, L, H, W, 3) uint8 -> (B, L, size, size) uint8 grayscale."""
    b, l = pov_bt.shape[:2]
    out = np.empty((b, l, size, size), dtype=np.uint8)
    for i in range(b):
        for t in range(l):
            g = cv2.cvtColor(pov_bt[i, t], cv2.COLOR_RGB2GRAY)
            if g.shape[0] != size:
                g = cv2.resize(g, (size, size), interpolation=cv2.INTER_AREA)
            out[i, t] = g
    return out


def stack_frames(gray, n):
    """(B, L, H, W) -> (B, L, n, H, W), padding the start by repeating frame 0."""
    b, l, h, w = gray.shape
    out = np.empty((b, l, n, h, w), dtype=gray.dtype)
    for i in range(b):
        for t in range(l):
            for k in range(n):
                out[i, t, k] = gray[i, max(0, t - (n - 1) + k)]
    return out


def labels_from_actions(action, b, l):
    """Turn a batched MineRL action dict into (B, L) discrete labels."""
    labels = np.zeros((b, l), dtype=np.int64)
    for i in range(b):
        for t in range(l):
            single = {k: action[k][i, t] for k in action}
            labels[i, t] = discretize_action(single)
    return labels


def main():
    cfg = Config()
    p = argparse.ArgumentParser()
    p.add_argument("--env", default="MineRLTreechop-v0")
    p.add_argument("--epochs", type=int, default=2)
    p.add_argument("--batch-size", type=int, default=16)
    p.add_argument("--seq-len", type=int, default=32)
    p.add_argument("--lr", type=float, default=3e-4)
    p.add_argument("--data-dir", default=os.environ.get("MINERL_DATA_ROOT", "data"))
    p.add_argument("--out", default="checkpoints/bc_policy.pt")
    args = p.parse_args()

    import minerl  # noqa: F401

    device, desc = get_device("auto")
    print(f"Device: {desc}")

    data = minerl.data.make(args.env, data_dir=args.data_dir)
    agent = ActorCritic(cfg.frame_stack, cfg.frame_size, NUM_ACTIONS).to(device)
    opt = torch.optim.Adam(agent.parameters(), lr=args.lr)
    loss_fn = nn.CrossEntropyLoss()

    step, running = 0, 0.0
    for obs, action, _reward, _next_obs, _done in data.batch_iter(
        batch_size=args.batch_size, seq_len=args.seq_len, num_epochs=args.epochs
    ):
        pov = obs["pov"]  # (B, L, H, W, 3) uint8
        b, l = pov.shape[:2]

        gray = to_gray(pov, cfg.frame_size)
        stacks = stack_frames(gray, cfg.frame_stack)              # (B, L, N, H, W)
        labels = labels_from_actions(action, b, l)               # (B, L)

        x = torch.as_tensor(stacks.reshape(b * l, cfg.frame_stack, cfg.frame_size, cfg.frame_size),
                            dtype=torch.float32, device=device) / 255.0
        y = torch.as_tensor(labels.reshape(b * l), device=device)

        logits, _, _ = agent(x)
        loss = loss_fn(logits, y)

        opt.zero_grad(set_to_none=True)
        loss.backward()
        nn.utils.clip_grad_norm_(agent.parameters(), 1.0)
        opt.step()

        step += 1
        running += loss.item()
        if step % 20 == 0:
            print(f"step {step}  bc_loss {running / 20:.4f}")
            running = 0.0

    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    torch.save({"agent": agent.state_dict(), "bc_env": args.env, "bc_steps": step}, args.out)
    print(f"Saved cloned policy -> {args.out}")
    print(f"Now reinforce it:  python train.py --env {args.env} --init-from {args.out}")


if __name__ == "__main__":
    main()
