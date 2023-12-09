package net.pierrox.lightning_launcher.di

import net.pierrox.lightning_launcher.util.FilesHolder
import org.koin.core.module.Module
import org.koin.dsl.module

object UtilitiesModule : FeatureModule {
    override val modules: List<Module>
        get() = listOf(
            filesHolderModule,
        )
}

private val filesHolderModule = module {
    factory {
        FilesHolder(context = get())
    }
}