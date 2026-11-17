# Worker connection status

Worker health returned ready:true. POST /resolve without a token returned 401. These checks do not prove successful DeepSeek API communication or playback.

Implemented default endpoint https://pooyan-tv-resolver.pooyan-ghorbani-real.workers.dev and persistent AES-GCM token encryption using Android Keystore. Legacy plaintext token migration is implemented and removes the old preference only after successful encrypted persistence. No DeepSeek API key is requested by Android. Build, unit tests, Worker mock tests and lint passed: https://github.com/PooyanGhorbani/Pooyan-TV/actions/runs/44049440733

The first in-place upgrade was rejected with INSTALL_FAILED_UPDATE_INCOMPATIBLE due to different Debug signatures. No application was removed. A separate package com.pooyan.namakadetv.preview042worker was built and installed successfully. Launch returned Status:ok, cold start 1061ms. AndroidRuntime reported no Fatal during this check. Existing installations and their data were preserved. New version settings were opened for private token entry; the token has not been inserted through shell commands or stored in this repository.

Verification pending private token entry: successful authenticated resolver request; actual DeepSeek API response; verified media discovery; FarsiPlex Media3 playback. Cloudflare account/log access is unavailable through an authorized connector. The health endpoint is not evidence for these pending items. Encrypted token persistence awaits a real save on the device and is not yet claimed as runtime-verified.

APK: https://github.com/PooyanGhorbani/Pooyan-TV/releases/download/v0.4.2/Pooyan-TV-v0.4.2-worker-update.apk
