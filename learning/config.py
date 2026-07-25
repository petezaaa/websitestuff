"""Hyper-parameters and run configuration for the curiosity RL agent.

Everything is a plain dataclass so you can tweak values here or override them
from the command line in train.py.
"""
from __future__ import annotations

from dataclasses import dataclass, field, asdict


@dataclass
class Config:
    # --- Environment --------------------------------------------------------
    # Any MineRL env id. Open-ended ones suit curiosity-driven free play:
    #   "MineRLBasaltFindCave-v0"      (open world, human action space)
    #   "MineRLObtainDiamondShovel-v0" (task with sparse reward)
    #   "MineRLTreechop-v0"            (simple, good for a first sanity run)
    env_id: str = "MineRLBasaltFindCave-v0"
    frame_size: int = 64          # POV is 64x64; kept square
    frame_stack: int = 4          # stack N frames so the net sees motion
    action_repeat: int = 4        # hold each action for N game ticks

    # --- Rollout / PPO ------------------------------------------------------
    rollout_steps: int = 512      # steps collected per update
    update_epochs: int = 4
    minibatch_size: int = 128
    gamma_ext: float = 0.999      # discount for extrinsic (game) reward
    gamma_int: float = 0.99       # discount for intrinsic (curiosity) reward
    gae_lambda: float = 0.95
    clip_coef: float = 0.2
    entropy_coef: float = 0.01    # encourages "random"/exploratory actions
    value_coef: float = 0.5
    max_grad_norm: float = 0.5
    learning_rate: float = 2.5e-4

    # --- Curiosity (Random Network Distillation) ---------------------------
    int_coef: float = 1.0         # weight on curiosity reward
    ext_coef: float = 2.0         # weight on real game reward
    rnd_lr: float = 1e-4
    rnd_update_proportion: float = 0.25  # fraction of samples used to train RND
    obs_norm_clip: float = 5.0

    # --- Training loop ------------------------------------------------------
    total_steps: int = 5_000_000
    seed: int = 0
    device: str = "cuda"          # falls back to cpu automatically if no GPU
    checkpoint_dir: str = "checkpoints"
    checkpoint_every_updates: int = 25
    log_dir: str = "runs"
    resume: bool = True           # continue from the latest checkpoint if present

    def to_dict(self) -> dict:
        return asdict(self)
