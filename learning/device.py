"""GPU/device selection that works across NVIDIA (CUDA), AMD (ROCm & DirectML).

PyTorch's ROCm build (Linux, AMD) still reports itself through the ``cuda``
device API, so an AMD RX 6800 on ROCm is used exactly like an NVIDIA card. On
Windows, AMD cards go through torch-directml instead, which exposes a separate
device object. This helper hides those differences.
"""
from __future__ import annotations

import torch


def get_device(prefer: str = "auto"):
    """Return (torch.device, human-readable description).

    prefer: "auto" (default), "cpu", "cuda"/"gpu", or "directml".
    """
    prefer = (prefer or "auto").lower()

    if prefer == "cpu":
        return torch.device("cpu"), "CPU only (forced)"

    # CUDA path also covers AMD ROCm — torch.version.hip is set on ROCm builds.
    if prefer in ("auto", "cuda", "gpu") and torch.cuda.is_available():
        try:
            name = torch.cuda.get_device_name(0)
        except Exception:
            name = "GPU"
        backend = "ROCm/HIP" if getattr(torch.version, "hip", None) else "CUDA"
        return torch.device("cuda"), f"{name} [{backend}]"

    # Windows AMD (and Intel) path: DirectML.
    if prefer in ("auto", "directml"):
        try:
            import torch_directml  # type: ignore

            dev = torch_directml.device()
            try:
                name = torch_directml.device_name(0)
            except Exception:
                name = "device 0"
            return dev, f"{name} [DirectML]"
        except Exception:
            pass

    return torch.device("cpu"), "CPU only (no GPU backend found)"
