package li.songe.gkd.root;

import li.songe.gkd.root.RootInputRequest;
import li.songe.gkd.root.RootForegroundTask;

/** Minimal structured handshake for the APK-owned root process. */
interface IRootService {
    int getProtocolVersion() = 1;
    int getRemotePid() = 2;
    int getRemoteUid() = 3;
    String getServicePackageName() = 4;
    int performInput(in RootInputRequest request) = 5;
    RootForegroundTask getForegroundTask(int displayId) = 6;
    @nullable String[] getDetectorFolderPackages() = 7;
    void publishNodePolicy(boolean enabled, in String[] excluded, in String[] allowed) = 8;
    boolean requestXSpaceScopeSetup() = 9;
    @nullable String[] getNodeScopePackages(int userId) = 10;
}
