package net.pierrox.lightning_launcher.di

import org.koin.core.module.Module

/**
 * Base interface which provides DI modules
 **/
interface FeatureModule {

    val modules: List<Module>

}