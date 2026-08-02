FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    ffmpeg \
    patchelf \
    && rm -rf /var/lib/apt/lists/*


# Copy requirements and install
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Google Compute Engine kernels can reject native libraries that request an
# executable stack. CTranslate2 is loaded by WhisperX at runtime, so clear the
# flag from its bundled shared libraries while building the image.
RUN set -eux; \
    find /usr/local/lib/python3.11/site-packages -type f \
        -name 'libctranslate2*.so*' -print -quit | grep -q .; \
    find /usr/local/lib/python3.11/site-packages -type f \
        -name 'libctranslate2*.so*' -print -exec patchelf --clear-execstack {} \;

# Copy source code
COPY main.py .

EXPOSE 8001

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
