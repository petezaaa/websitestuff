"""Verify PyTorch can see and use your GPU before touching MineRL.

    python check_gpu.py

Prints your torch build, the detected device (CUDA / AMD ROCm / DirectML / CPU),
and runs a small matmul on the device to confirm compute actually works. Handy
for confirming a ROCm install on the RX 6800 in isolation.
"""
from __future__ import annotations

import time


def main():
    try:
        import torch
    except ImportError:
        print("PyTorch is not installed. Install it for your GPU first — see README.md.")
        return

    print(f"torch version : {torch.__version__}")
    print(f"built with HIP: {getattr(torch.version, 'hip', None)}  (set => this is a ROCm/AMD build)")
    print(f"built with CUDA: {getattr(torch.version, 'cuda', None)}")
    print(f"torch.cuda.is_available(): {torch.cuda.is_available()}")

    from device import get_device

    device, desc = get_device("auto")
    print(f"\nSelected device: {device}  ->  {desc}")

    if str(device) == "cpu":
        print("\nNo GPU backend was found. For the RX 6800:")
        print("  Linux : pip install torch --index-url https://download.pytorch.org/whl/rocm6.1")
        print("  Windows: pip install torch-directml   (then run with --device directml)")
        return

    # Small compute test on the device.
    try:
        n = 4096
        a = torch.randn(n, n, device=device)
        b = torch.randn(n, n, device=device)
        # warm up (kernel compile / allocation)
        (a @ b).sum().item()

        start = time.time()
        iters = 10
        for _ in range(iters):
            c = a @ b
        # force sync + read a value back
        result = float(c.sum().item())
        elapsed = time.time() - start

        flops = 2.0 * n ** 3 * iters
        tflops = flops / elapsed / 1e12
        print(f"\nMatmul {n}x{n} x{iters}: {elapsed:.3f}s  (~{tflops:.1f} TFLOP/s), checksum={result:.1f}")
        print("GPU compute works. You're good to set up MineRL and run train.py.")
    except Exception as e:  # noqa: BLE001
        print(f"\nGPU compute FAILED: {type(e).__name__}: {e}")
        print("If this is an AMD ROCm 'invalid device function' error, try:")
        print("  export HSA_OVERRIDE_GFX_VERSION=10.3.0")
        print("and run this again.")


if __name__ == "__main__":
    main()
