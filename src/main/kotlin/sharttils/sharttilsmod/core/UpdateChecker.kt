/*
 * Sharttils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2022 Sharttils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package sharttils.sharttilsmod.core

import com.google.gson.JsonObject
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.util.Util
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.io.entity.EntityUtils
import sharttils.sharttilsmod.Sharttils
import sharttils.sharttilsmod.gui.RequestUpdateGui
import sharttils.sharttilsmod.gui.UpdateGui
import sharttils.sharttilsmod.utils.APIUtil
import sharttils.sharttilsmod.utils.Utils
import java.awt.Desktop
import java.io.File
import java.net.URL
import kotlin.concurrent.thread

object UpdateChecker {
    val updateGetter = UpdateGetter()
    val updateAsset
        get() = updateGetter.updateObj!!["assets"].asJsonArray[0].asJsonObject
    val updateDownloadURL: String
        get() = updateAsset["browser_download_url"].asString

    fun getJarNameFromUrl(url: String): String {
        return url.split(Regex("/")).last()
    }

    fun scheduleCopyUpdateAtShutdown(jarName: String) {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                println("Attempting to apply Sharttils update.")
                val oldJar = Sharttils.jarFile
                if (oldJar == null || !oldJar.exists() || oldJar.isDirectory) {
                    println("Old jar file not found.")
                    return@Thread
                }
                println("Copying updated jar to mods.")
                val newJar = File(File(Sharttils.modDir, "updates"), jarName)
                println("Copying to mod folder")
                val nameNoExtension = jarName.substringBeforeLast(".")
                val newExtension = jarName.substringAfterLast(".")
                val newLocation = File(
                    oldJar.parent,
                    "${if (oldJar.name.startsWith("!")) "!" else ""}${nameNoExtension}${if (oldJar.endsWith(".temp.jar") && newExtension == oldJar.extension) ".temp.jar" else ".$newExtension"}"
                )
                newLocation.createNewFile()
                newJar.copyTo(newLocation, true)
                newJar.delete()
                if (oldJar.delete()) {
                    println("successfully deleted the files. skipping install tasks")
                    return@Thread
                }
                println("Running delete task")
                val taskFile = File(File(Sharttils.modDir, "updates"), "tasks").listFiles()?.last()
                if (taskFile == null) {
                    println("Task doesn't exist")
                    return@Thread
                }
                val runtime = Utils.getJavaRuntime()
                if (Util.getOSType() == Util.EnumOS.OSX) {
                    val sipStatus = Runtime.getRuntime().exec("csrutil status")
                    sipStatus.waitFor()
                    if (!sipStatus.inputStream.use { it.bufferedReader().readText() }
                            .contains("System Integrity Protection status: disabled.")) {
                        println("SIP is NOT disabled, opening Finder.")
                        Desktop.getDesktop().open(oldJar.parentFile)
                        return@Thread
                    }
                }
                println("Using runtime $runtime")
                Runtime.getRuntime().exec("\"$runtime\" -jar \"${taskFile.absolutePath}\" \"${oldJar.absolutePath}\"")
                println("Successfully applied Sharttils update.")
            } catch (ex: Throwable) {
                println("Failed to apply Sharttils Update.")
                ex.printStackTrace()
            }
        })
    }

    fun downloadDeleteTask() {
        Thread {
            println("Checking for Sharttils install task...")
            val taskDir = File(File(Sharttils.modDir, "updates"), "tasks")
            // TODO Make this dynamic and fetch latest one or something
            val url =
                "https://github.com/Sharttils/SharttilsMod-Data/releases/download/files/SharttilsInstaller-1.1.1.jar"
            val taskFile = File(taskDir, getJarNameFromUrl(url))
            if (taskDir.mkdirs() || taskFile.createNewFile()) {
                println("Downloading Sharttils delete task.")
                val client = APIUtil.builder.build()

                val req = HttpGet(URL(url).toURI())
                val res = client.execute(req)
                if (res.code != 200) {
                    println("Downloading delete task failed!")
                } else {
                    println("Writing Sharttils delete task.")
                    res.entity.writeTo(taskFile.outputStream())
                    EntityUtils.consume(res.entity)
                    client.close()
                    println("Delete task successfully downloaded!")
                }
            }
        }.start()
    }

    init {
        try {
            thread(block = updateGetter::run).join()
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onGuiOpen(e: GuiOpenEvent) {
        if (e.gui !is GuiMainMenu) return
        if (updateGetter.updateObj == null) return
        if (UpdateGui.complete) return
        Sharttils.displayScreen = RequestUpdateGui()
    }

    class UpdateGetter : Runnable {
        @Volatile
        var updateObj: JsonObject? = null

        override fun run() {
            println("Checking for updates...")
            val latestRelease = when (Sharttils.config.updateChannel) {
                2 -> APIUtil.getJSONResponse(
                    "https://api.github.com/repos/Sharttils/SharttilsMod/releases/latest"
                )
                1 -> APIUtil.getArrayResponse(
                    "https://api.github.com/repos/Sharttils/SharttilsMod/releases"
                )[0].asJsonObject
                else -> return println("Channel set as none")
            }
            val latestTag = latestRelease["tag_name"].asString
            val currentTag = Sharttils.VERSION.substringBefore("-dev")

            val currentVersion = SharttilsVersion(currentTag)
            val latestVersion = SharttilsVersion(latestTag.substringAfter("v"))
            if (currentVersion < latestVersion) {
                updateObj = latestRelease
            }
        }
    }

    class SharttilsVersion(val versionString: String) : Comparable<SharttilsVersion> {

        companion object {
            val regex by lazy { Regex("^(?<version>[\\d.]+)-?(?<type>\\D+)?(?<typever>\\d+\\.?\\d*)?\$") }
        }

        private val matched by lazy { regex.find(versionString) }
        val isSafe by lazy { matched != null }

        val version by lazy { matched!!.groups["version"]!!.value }
        val versionArtifact by lazy { DefaultArtifactVersion(matched!!.groups["version"]!!.value) }
        val specialVersionType by lazy {
            val typeString = matched!!.groups["type"]?.value ?: return@lazy UpdateType.RELEASE

            return@lazy UpdateType.values().find { typeString == it.prefix } ?: UpdateType.UNKNOWN
        }
        val specialVersion by lazy {
            if (specialVersionType == UpdateType.RELEASE) return@lazy null
            return@lazy matched!!.groups["typever"]?.value?.toDoubleOrNull()
        }

        override fun compareTo(other: SharttilsVersion): Int {
            if (!isSafe || !other.isSafe) return -1
            return if (versionArtifact.compareTo(other.versionArtifact) == 0) {
                if (specialVersionType.ordinal == other.specialVersionType.ordinal) {
                    (specialVersion ?: 0.0).compareTo(other.specialVersion ?: 0.0)
                } else other.specialVersionType.ordinal - specialVersionType.ordinal
            } else versionArtifact.compareTo(other.versionArtifact)
        }
    }

    enum class UpdateType(val prefix: String) {
        UNKNOWN("unknown"),
        RELEASE(""),
        RELEASECANDIDATE("RC"),
        PRERELEASE("pre"),
    }
}