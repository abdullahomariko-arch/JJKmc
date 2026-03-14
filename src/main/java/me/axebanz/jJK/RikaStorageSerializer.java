package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Base64;

public final class RikaStorageSerializer {

    private RikaStorageSerializer() {}

    public static String toBase64(Inventory inv) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {

            oos.writeInt(inv.getSize());
            for (int i = 0; i < inv.getSize(); i++) {
                oos.writeObject(inv.getItem(i));
            }
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static Inventory fromBase64(String base64, int defaultSize, String title) {
        if (base64 == null || base64.isBlank()) {
            return Bukkit.createInventory(null, defaultSize, title);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {

            int size = ois.readInt();
            Inventory inv = Bukkit.createInventory(null, size, title);
            for (int i = 0; i < size; i++) {
                Object obj = ois.readObject();
                inv.setItem(i, (ItemStack) obj);
            }
            return inv;
        } catch (Exception e) {
            return Bukkit.createInventory(null, defaultSize, title);
        }
    }
}