"""MineRL environment wrappers.

Turns MineRL's dict observation/action spaces into something a small CNN agent
can consume: a stack of grayscale POV frames in, a single discrete action out.
The discrete action set only sets keys that actually exist in the chosen env's
action space, so the same code works across MineRL tasks.
"""
from __future__ import annotations

from collections import deque

import cv2
import gym
import numpy as np


# Each entry: buttons to press (value 1) plus an optional camera [pitch, yaw].
CAM = 10.0
DISCRETE_ACTIONS = [
    {},                                             # 0 no-op
    {"forward": 1},                                 # 1
    {"forward": 1, "jump": 1},                      # 2
    {"forward": 1, "sprint": 1},                    # 3
    {"back": 1},                                    # 4
    {"left": 1},                                    # 5
    {"right": 1},                                   # 6
    {"jump": 1},                                    # 7
    {"attack": 1},                                  # 8
    {"use": 1},                                     # 9
    {"camera": [0.0, -CAM]},                        # 10 turn left
    {"camera": [0.0, CAM]},                         # 11 turn right
    {"camera": [-CAM, 0.0]},                        # 12 look up
    {"camera": [CAM, 0.0]},                         # 13 look down
    {"forward": 1, "attack": 1},                    # 14
]
NUM_ACTIONS = len(DISCRETE_ACTIONS)


def discretize_action(action) -> int:
    """Map a MineRL human action (dict of buttons + a camera delta) to the
    nearest of our discrete actions. Used to turn demo data into labels for
    behavioral cloning. Priority order roughly matches what dominates a frame.
    """
    def flag(key):
        v = action.get(key, 0)
        try:
            return int(np.asarray(v).flatten()[0])
        except Exception:
            return int(bool(v))

    cam = np.asarray(action.get("camera", [0.0, 0.0])).flatten()
    pitch = float(cam[0]) if cam.size > 0 else 0.0
    yaw = float(cam[1]) if cam.size > 1 else 0.0
    cam_thresh = 5.0

    attack, use = flag("attack"), flag("use")
    forward, back = flag("forward"), flag("back")
    left, right = flag("left"), flag("right")
    jump, sprint = flag("jump"), flag("sprint")

    if attack and forward:
        return 14
    if attack:
        return 8
    if use:
        return 9
    if abs(yaw) > cam_thresh and abs(yaw) >= abs(pitch):
        return 11 if yaw > 0 else 10
    if abs(pitch) > cam_thresh:
        return 13 if pitch > 0 else 12
    if forward and jump:
        return 2
    if forward and sprint:
        return 3
    if forward:
        return 1
    if back:
        return 4
    if left:
        return 5
    if right:
        return 6
    if jump:
        return 7
    return 0


class GrayResizePOV(gym.ObservationWrapper):
    """dict obs -> single grayscale HxW uint8 frame from the POV image."""

    def __init__(self, env, size: int = 64):
        super().__init__(env)
        self.size = size
        self.observation_space = gym.spaces.Box(0, 255, (size, size), np.uint8)

    def observation(self, obs):
        pov = obs["pov"] if isinstance(obs, dict) else obs
        gray = cv2.cvtColor(pov, cv2.COLOR_RGB2GRAY)
        if gray.shape[0] != self.size:
            gray = cv2.resize(gray, (self.size, self.size), interpolation=cv2.INTER_AREA)
        return gray.astype(np.uint8)


class FrameStack(gym.Wrapper):
    """Stack the last N frames along a new channel axis -> (N, H, W) uint8."""

    def __init__(self, env, n: int = 4):
        super().__init__(env)
        self.n = n
        self.frames = deque(maxlen=n)
        h, w = env.observation_space.shape
        self.observation_space = gym.spaces.Box(0, 255, (n, h, w), np.uint8)

    def reset(self, **kwargs):
        obs = self.env.reset(**kwargs)
        for _ in range(self.n):
            self.frames.append(obs)
        return self._stack()

    def step(self, action):
        obs, reward, done, info = self.env.step(action)
        self.frames.append(obs)
        return self._stack(), reward, done, info

    def _stack(self):
        return np.stack(self.frames, axis=0)


class DiscreteActions(gym.ActionWrapper):
    """Map a discrete action index onto the env's noop action dict."""

    def __init__(self, env):
        super().__init__(env)
        self.action_space = gym.spaces.Discrete(NUM_ACTIONS)
        self._noop = env.action_space.noop()

    def action(self, index: int):
        act = self._noop.copy()
        spec = DISCRETE_ACTIONS[int(index)]
        for key, value in spec.items():
            if key == "camera":
                if "camera" in act:
                    act["camera"] = np.array(value, dtype=np.float32)
            elif key in act:
                act[key] = value
        return act


class ActionRepeat(gym.Wrapper):
    """Hold each chosen action for k ticks, summing reward (frame-skip)."""

    def __init__(self, env, k: int = 4):
        super().__init__(env)
        self.k = k

    def step(self, action):
        total = 0.0
        done = False
        info = {}
        obs = None
        for _ in range(self.k):
            obs, reward, done, info = self.env.step(action)
            total += reward
            if done:
                break
        return obs, total, done, info


def make_env(cfg):
    """Build the wrapped MineRL environment described by cfg."""
    import minerl  # noqa: F401  (registers the MineRL gym ids on import)

    env = gym.make(cfg.env_id)
    env = DiscreteActions(env)
    env = GrayResizePOV(env, size=cfg.frame_size)
    env = ActionRepeat(env, k=cfg.action_repeat)
    env = FrameStack(env, n=cfg.frame_stack)
    env.seed(cfg.seed)
    return env
