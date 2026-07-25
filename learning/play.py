"""Watch a trained agent play (no learning).

    python play.py --checkpoint checkpoints/latest.pt --episodes 3

Loads the policy from a checkpoint and acts greedily-ish (samples from the
policy) so you can see what it has learned. Rendering depends on your MineRL
setup; rewards are printed regardless.
"""
from __future__ import annotations

import argparse

import numpy as np
import torch

from config import Config
from device import get_device
from env import make_env, NUM_ACTIONS
from model import ActorCritic


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--checkpoint", default="checkpoints/latest.pt")
    p.add_argument("--env", default=None)
    p.add_argument("--episodes", type=int, default=3)
    p.add_argument("--render", action="store_true")
    args = p.parse_args()

    cfg = Config()
    if args.env:
        cfg.env_id = args.env

    device, desc = get_device("auto")
    print(f"Device: {desc}")
    ck = torch.load(args.checkpoint, map_location=device)

    env = make_env(cfg)
    agent = ActorCritic(cfg.frame_stack, cfg.frame_size, NUM_ACTIONS).to(device)
    agent.load_state_dict(ck["agent"])
    agent.eval()
    print(f"Loaded {args.checkpoint} (trained {ck.get('global_step', '?')} steps).")

    for ep in range(args.episodes):
        obs = env.reset()
        done = False
        total = 0.0
        while not done:
            obs_t = torch.as_tensor(obs[None], dtype=torch.float32, device=device) / 255.0
            with torch.no_grad():
                action, _, _, _, _ = agent.get_action_and_value(obs_t)
            obs, reward, done, _ = env.step(int(action.item()))
            total += reward
            if args.render:
                try:
                    env.render()
                except Exception:
                    pass
        print(f"episode {ep + 1}: reward {total:.2f}")

    env.close()


if __name__ == "__main__":
    main()
