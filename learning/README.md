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
env.py                 MineRL wrappers + human-action -> discrete mapping
model.py               CNN actor-critic (policy + 2 value heads)
rnd.py                 Random Network Distillation (intrinsic reward)
utils.py               running mean/std normalisation
device.py              GPU auto-detect (CUDA / AMD ROCm / DirectML / CPU)
config.py              all hyper-parameters
train.py               rollout + PPO/RND update loop, checkpoints, TensorBoard
behavioral_cloning.py  warm-start the policy from human demo data
play.py                load a checkpoint and watch it act
check_gpu.py           verify torch sees & can compute on your GPU
```

## Setup

You need **Python 3.9–3.11**, **JDK 8** on the PATH (MineRL launches Minecraft
1.16), and a GPU. The device layer auto-detects NVIDIA (CUDA), **AMD (ROCm on
Linux / DirectML on Windows)**, or falls back to CPU.

```bash
cd learning
python -m venv .venv && source .venv/bin/activate
```

Then install **PyTorch for your GPU** (do this *before* `requirements.txt`, and
don't let anything reinstall a CPU-only torch over it):

### AMD RX 6800 — Linux (recommended: ROCm)

The RX 6800 is RDNA2 / `gfx1030`, supported by recent ROCm. Install the ROCm
build of PyTorch (pick the rocm channel matching your installed ROCm; 6.1 shown):

```bash
pip install torch --index-url https://download.pytorch.org/whl/rocm6.1
pip install -r requirements.txt

# ROCm shows up as the "cuda" device in torch — the code already handles that.
python -c "import torch; print(torch.cuda.is_available(), torch.version.hip)"

# If you hit "invalid device function" / HIP errors, force the gfx1030 target:
export HSA_OVERRIDE_GFX_VERSION=10.3.0
python train.py --env MineRLTreechop-v0
```

### AMD RX 6800 — Windows (DirectML)

ROCm isn't available on Windows for this card, so use DirectML:

```bash
pip install torch-directml        # pins a compatible torch build
pip install -r requirements.txt
python train.py --env MineRLTreechop-v0 --device directml
```

DirectML works but is slower than ROCm and a few ops may be unsupported; if
training errors on an op, try `--device cpu` to confirm the rest runs, or switch
to Linux+ROCm for real speed. (Honestly, for RL throughput, **Linux + ROCm on
the RX 6800 is the better setup**.)

### NVIDIA (for reference)

```bash
pip install torch --index-url https://download.pytorch.org/whl/cu121
pip install -r requirements.txt
```

MineRL can be fiddly (it compiles a Minecraft mod). If `pip install minerl`
fails, see https://minerl.readthedocs.io/ — usual culprits are a missing JDK 8
or a headless machine needing a virtual display (`xvfb-run -a python train.py ...`).

## Verify the GPU first

Before the (fiddly) MineRL install, confirm PyTorch actually sees and can
compute on your RX 6800:

```bash
python check_gpu.py
```

It prints your torch build, the detected device (should say `[ROCm/HIP]` for the
RX 6800 on Linux, or `[DirectML]` on Windows), and runs a small matmul on the
GPU to prove compute works — with the `HSA_OVERRIDE_GFX_VERSION=10.3.0` tip if it
errors.

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

## Warm-start from human demos (behavioral cloning)

Learning everything from scratch is slow. MineRL ships **human demonstration
data**; cloning it first gives the agent sensible behaviour to build on, then
RL + curiosity refine it. This is the single biggest speed-up you can add.

```bash
# 1) download the demos for your env (a few GB, one time)
python -m minerl.data.download --environment MineRLTreechop-v0

# 2) clone a policy from the humans
python behavioral_cloning.py --env MineRLTreechop-v0 --epochs 2

# 3) reinforce the cloned policy (curiosity + reward take over)
python train.py --env MineRLTreechop-v0 --init-from checkpoints/bc_policy.pt
```

`behavioral_cloning.py` maps each human action to our discrete action set and
trains the policy with supervised cross-entropy. `--init-from` loads those
weights into `train.py` (RND and the value heads then train fresh). Works with
v0.4-style envs whose demo actions are button flags + a `camera` delta
(Treechop, ObtainDiamond, BASALT FindCave).

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
