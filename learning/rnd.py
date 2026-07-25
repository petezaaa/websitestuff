"""Random Network Distillation (RND) — the curiosity signal.

A fixed, randomly-initialised *target* network embeds each new observation.
A *predictor* network is trained to match it. Where the predictor is wrong, the
state is novel, so the prediction error is used as an intrinsic reward. As the
agent revisits states the predictor catches up and the novelty fades — which is
what makes the agent seek out new things ("be curious").
"""
from __future__ import annotations

import numpy as np
import torch
import torch.nn as nn


class RNDModel(nn.Module):
    def __init__(self, frame_size: int, feature_dim: int = 512):
        super().__init__()
        self.target = self._make_net(frame_size, feature_dim)
        self.predictor = self._make_net(frame_size, feature_dim)
        # The target is frozen forever.
        for p in self.target.parameters():
            p.requires_grad = False

    @staticmethod
    def _make_net(frame_size: int, feature_dim: int) -> nn.Module:
        conv = nn.Sequential(
            nn.Conv2d(1, 32, 8, stride=4), nn.LeakyReLU(),
            nn.Conv2d(32, 64, 4, stride=2), nn.LeakyReLU(),
            nn.Conv2d(64, 64, 3, stride=1), nn.LeakyReLU(),
            nn.Flatten(),
        )
        with torch.no_grad():
            n_flat = conv(torch.zeros(1, 1, frame_size, frame_size)).shape[1]
        return nn.Sequential(conv, nn.Linear(n_flat, feature_dim))

    def forward(self, next_obs_single):
        """next_obs_single: (B, 1, H, W) normalised single frame."""
        target_feat = self.target(next_obs_single)
        pred_feat = self.predictor(next_obs_single)
        return pred_feat, target_feat

    def intrinsic_reward(self, next_obs_single) -> torch.Tensor:
        with torch.no_grad():
            pred, target = self.forward(next_obs_single)
            return ((target - pred) ** 2).mean(dim=1)

    def predictor_loss(self, next_obs_single, keep_mask=None) -> torch.Tensor:
        pred, target = self.forward(next_obs_single)
        per_sample = ((pred - target.detach()) ** 2).mean(dim=1)
        if keep_mask is not None:
            per_sample = per_sample * keep_mask
            denom = keep_mask.sum().clamp(min=1.0)
            return per_sample.sum() / denom
        return per_sample.mean()
