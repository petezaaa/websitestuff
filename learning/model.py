"""The actor-critic network.

A Nature-DQN style CNN encoder feeds a categorical policy head and TWO value
heads: one for extrinsic (game) reward and one for intrinsic (curiosity)
reward, as in the RND paper. Keeping the two value streams separate lets the
agent value "surprise" on a different timescale than real rewards.
"""
from __future__ import annotations

import numpy as np
import torch
import torch.nn as nn
from torch.distributions import Categorical


def _orthogonal(layer, gain=np.sqrt(2)):
    nn.init.orthogonal_(layer.weight, gain)
    if layer.bias is not None:
        nn.init.constant_(layer.bias, 0.0)
    return layer


class NatureCNN(nn.Module):
    def __init__(self, in_channels: int, frame_size: int, features: int = 512):
        super().__init__()
        self.conv = nn.Sequential(
            _orthogonal(nn.Conv2d(in_channels, 32, 8, stride=4)), nn.ReLU(),
            _orthogonal(nn.Conv2d(32, 64, 4, stride=2)), nn.ReLU(),
            _orthogonal(nn.Conv2d(64, 64, 3, stride=1)), nn.ReLU(),
            nn.Flatten(),
        )
        with torch.no_grad():
            dummy = torch.zeros(1, in_channels, frame_size, frame_size)
            n_flat = self.conv(dummy).shape[1]
        self.fc = nn.Sequential(_orthogonal(nn.Linear(n_flat, features)), nn.ReLU())

    def forward(self, x):
        return self.fc(self.conv(x))


class ActorCritic(nn.Module):
    def __init__(self, in_channels: int, frame_size: int, num_actions: int):
        super().__init__()
        self.encoder = NatureCNN(in_channels, frame_size)
        self.actor = _orthogonal(nn.Linear(512, num_actions), gain=0.01)
        self.critic_ext = _orthogonal(nn.Linear(512, 1), gain=1.0)
        self.critic_int = _orthogonal(nn.Linear(512, 1), gain=1.0)

    def forward(self, obs):
        feat = self.encoder(obs)
        return self.actor(feat), self.critic_ext(feat).squeeze(-1), self.critic_int(feat).squeeze(-1)

    def get_action_and_value(self, obs, action=None):
        logits, v_ext, v_int = self.forward(obs)
        dist = Categorical(logits=logits)
        if action is None:
            action = dist.sample()
        return action, dist.log_prob(action), dist.entropy(), v_ext, v_int
