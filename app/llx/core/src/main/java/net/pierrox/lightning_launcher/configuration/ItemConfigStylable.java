package net.pierrox.lightning_launcher.configuration;

public interface ItemConfigStylable {
    ItemConfig getItemConfig();

    void setItemConfig(ItemConfig c);

    ItemConfig modifyItemConfig();
}
