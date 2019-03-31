class Device {

    var serial = ""
    var codename = ""
    var bootloader = false
    var anti = 0
    var auth = false
    var dpi = -1
    var width = -1
    var height = -1
    var comm = Command()
    var recovery = false
    var reinstaller = true
    var mode = 0

    fun readADB(): Boolean {
        var op = comm.exec("adb get-serialno", true)
        auth = false
        if ("no devices" in op) {
            mode = 0
            return false
        }
        if ("unauthorized" in op) {
            mode = 0
            auth = true
            return false
        }
        recovery = comm.exec("adb devices").contains("recovery")
        op = comm.exec("adb get-serialno", false).trim()
        if (mode == 1 && op == serial && dpi != -1)
            return true
        serial = op
        codename = comm.exec("adb shell getprop ro.build.product", false).trim()
        bootloader = comm.exec("adb shell getprop ro.boot.flash.locked", false).contains("0") ||
                comm.exec("adb shell getprop ro.secureboot.lockstate", false).contains("unlocked")
        anti = -1
        if (!recovery) {
            op = comm.exec("adb shell wm density")
            dpi = try {
                op.substring(op.lastIndexOf(':') + 2).trim().toInt()
            } catch (e: Exception) {
                -1
            }
            op = comm.exec("adb shell wm size")
            width = try {
                op.substring(op.lastIndexOf(':') + 2, op.lastIndexOf('x')).toInt()
            } catch (e: Exception) {
                -1
            }
            height = try {
                op.substring(op.lastIndexOf('x') + 1).trim().toInt()
            } catch (e: Exception) {
                -1
            }
        }
        mode = 1
        return true
    }

    fun readFastboot(): Boolean {
        var op = comm.exec("fastboot devices", false)
        if (op.isEmpty()) {
            mode = 0
            return false
        }
        if (mode == 2 && op.contains(serial))
            return true
        recovery = false
        serial = op.substring(0, op.indexOf("fast")).trim()
        op = comm.exec("fastboot getvar product", true)
        codename = op.substring(9, op.indexOf(System.lineSeparator())).trim()
        bootloader = comm.exec("fastboot oem device-info", true).contains("unlocked: true")
        op = comm.exec("fastboot getvar anti", true)
        op = op.substring(0, op.indexOf(System.lineSeparator()))
        anti = if (op.length != 7)
            -1
        else op.substring(6).toInt()
        mode = 2
        return true
    }
}
