# LightningLauncher

This is a fork of the [Original Lightning Launcher repository](https://github.com/pierrehebert/LightningLauncher).
Even though it may be considered the 'primary fork' (as I helped Pierre a bit with the app) I'm unfortunately not very active 'in the development' of it. But I can try to assist with any issue if needed. Feel free to open an issue, and I'll try to assist if I can.

The differences between the original are:

- [developer branch](https://github.com/TrianguloY/LightningLauncher/tree/developer): Where ideally a new version/update of Lightning launcher for newer android devices should happen. Currently it only contains changes from SnowVolf that I have reviewed. It's not yet prepared as a replacement, although it should be stable. The main issue is that you cannot create backups with it (you can restore them though). You can try it if you want from the [Debug apks pre-release](https://github.com/TrianguloY/LightningLauncher/releases/tag/debug). This version is not signed.

- [patch](https://github.com/TrianguloY/LightningLauncher/releases/tag/patch): This is a minimal-change from the original code to remove the Play Store check and allow to continue using the app (it was removed from Play Store so the original apk is locked in a trial state). This is intended as a drop-in replacement, but unfortunately you need to uninstall the original apk. Remember to create a backup first (if you still can)! Or alternatively use [adb](https://github.com/TrianguloY/LightningLauncher/issues/24#issuecomment-2016823553) to replace the app without removing the data. Alternatively use the patch (community). This version is signed with my own developer keys.
- [patch (community)](https://github.com/TrianguloY/LightningLauncher/releases/tag/patch_community): This is the same as the patch version, but with an additional change to use a different package id, which allows it to install alongside the original (for those of us who still want to keep it). If you have already uninstalled Pierre's version, or you don't mind uninstalling it, the patched version should be preferred, since this one may break some scripts. This version is signed with my own developer keys.
- [extra permissions](https://github.com/TrianguloY/LightningLauncher/releases/tag/permissions): The extra permissions, which you need instead of the original since they require to be signed with the same keys. If you don't know what this is, you never used it so you can ignore it. Note that this is a single package with all the permissions, instead of one package for each. You should be able to enable/disable individual ones from Android settings. This version is signed with my own developer keys.
- [extra permissions (community)](https://github.com/TrianguloY/LightningLauncher/releases/tag/permissions_community): The extra permissions apk for the community version. 

