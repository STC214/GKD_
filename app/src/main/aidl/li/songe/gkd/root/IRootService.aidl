package li.songe.gkd.root;

import li.songe.gkd.root.RootInputRequest;

/** Minimal structured handshake for the APK-owned root process. */
interface IRootService {
    int getProtocolVersion() = 1;
    int getRemotePid() = 2;
    int getRemoteUid() = 3;
    String getServicePackageName() = 4;
    int performInput(in RootInputRequest request) = 5;
}
