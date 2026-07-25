# learning/ — curiosity-driven RL agent (GPU)

A from-scratch reinforcement-learning agent that learns to play Minecraft from
the raw screen, driven by **curiosity**. It uses your **GPU** (PyTorch/CUDA),
learns continuously, and — because it's rewarded for *surprise* — pokes around
and does novel/random things instead of sitting still.

This is a **research scaffold**, not a finished brain. Read "Reality check"
below before expecting it to speedrun anything.

## How it works

- **PPO** (Proximal Policy Optimization) trains the policy — a small CNN that
  looks at a stack of grayscale POV frames and picks one of ~15 discrete
  actions (move, turn, jump, attack, use…).
- **RND** (Random Network Distillation) is the curiosity signal. A frozen random
  network embeds each new frame; a second network learns to predict that
  embedding. Where it predicts badly, the state is *novel*, so the agent gets an
  intrinsic reward. As it revisits places the novelty fades — so it keeps
  seeking out new things. This is the "be curious / do random stuff" part, and
  it works even when the game gives no reward at all.
- Two value heads (extrinsic game reward + intrinsic curiosity) let it weigh
  real goals and curiosity on different timescales.
- **Checkpoints + TensorBoard**: it saves to `checkpoints/latest.pt` and resumes
  automatically, so it "learns as it goes" across runs.

```
env.py     MineRL wrappers: grayscale POV, frame-stack, discrete actions
model.py   CNN actor-critic (policy + 2 value heads)
rnd.py     Random Network Distillation (intrinsic reward)
utils.py   running mean/std normalisation
config.py  all hyper-parameters
train.py   rollout + PPO/RND update loop, checkpoints, TensorBoard
play.py    load a checkpoint and watch it act
```

## Setup

You need a machine with an **NVIDIA GPU** (CUDA), **Python 3.9–3.11**, and
**JDK 8** on the PATH (MineRL launches Minecraft 1.16 under the hood).

```bash
cd learning
python -m venv .venv && source .venv/bin/activate

# 1) PyTorch for YOUR CUDA version (see https://pytorch.org/get-started/locally/)
pip install torch --index-url https://download.pytorch.org/whl/cu121

# 2) the rest
pip install -r requirements.txt
```

MineRL can be fiddly to install (it compiles a Minecraft mod). If `pip install
minerl` fails, follow the official docs: https://minerl.readthedocs.io/ — the
usual culprits are a missing JDK 8 or a headless machine without a virtual
display (`xvfb-run -a python train.py ...`).

## Run

```bash
# Train (uses the GPU automatically; Ctrl-C any time — it checkpoints)
python train.py --env MineRLBasaltFindCave-v0

# Watch progress
tensorboard --logdir runs

# Watch a trained agent play
python play.py --checkpoint checkpoints/latest.pt --render
```

Good first environments:
- `MineRLTreechop-v0` — simplest, quickest signal that learning works.
- `MineRLBasaltFindCave-v0` — open world, great for pure curiosity.
- `MineRLObtainDiamondShovel-v0` — long-horizon task with sparse reward.

Everything (env id, learning rate, curiosity weight, rollout size, …) is in
`config.py` or overridable via `train.py` flags.

## Reality check

Learning Minecraft from pixels is genuinely hard — this is the same problem
OpenAI's VPT and DreamerV3 tackled with large compute budgets.

- On a single GPU, expect it to take **many hours to days** just to show clear,
  simple behaviours (e.g. reliably chopping a tree). Curiosity makes it *explore*
  quickly; turning exploration into *competence* is the slow part.
- It will **not** out-of-the-box get diamonds or beat the game. Reaching a
  diamond via pure RL is a known research milestone, not a weekend result.
- Nothing here runs inside the Claude sandbox — no GPU, and it can't launch
  Minecraft. The Python was syntax-checked, but you run and train it on your own
  machine, and you may need to adjust the MineRL version/action mapping to match
  the env you pick.

## Relationship to the mod

This is **separate** from the Baritone mod in the repo root. The mod is
hand-written, command-driven behaviour on Minecraft 1.20.1 (Forge). This is a
neural agent that *learns* behaviour on Minecraft 1.16 (MineRL). They don't
share a runtime — pick the one that matches what you want: scripted-and-capable,
or learning-from-scratch-and-curious.
