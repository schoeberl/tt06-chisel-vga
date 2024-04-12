import chisel3._
import chisel3.util._

/**
 * Example design in Chisel.
 * A redesign of the Tiny Tapeout example.
 */
class ChiselTop() extends Module {
  val io = IO(new Bundle {
    val ui_in = Input(UInt(8.W))      // Dedicated inputs
    val uo_out = Output(UInt(8.W))    // Dedicated outputs
    val uio_in = Input(UInt(8.W))     // IOs: Input path
    val uio_out = Output(UInt(8.W))   // IOs: Output path
    val uio_oe = Output(UInt(8.W))    // IOs: Enable path (active high: 0=input, 1=output)
    val ena = Input(Bool())           // will go high when the design is enabled
  })

  io.uio_out := 0.U
  // use bi-directionals as input
  io.uio_oe := 0.U

  val add = WireDefault(0.U(7.W))
  add := io.ui_in + io.uio_in

  // Blink with 1 Hz, to show that the design is running
  val cntReg = RegInit(0.U(32.W))
  val ledReg = RegInit(0.U(1.W))
  cntReg := cntReg + 1.U
  when (cntReg === 25000000.U) {
    cntReg := 0.U
    ledReg := ~ledReg
  }
  io.uo_out := ledReg ## add

  val hSync = WireDefault(false.B)
  val vSync = WireDefault(false.B)

  val on = WireDefault(false.B) // the display on/off signal

  // standard: http://tinyvga.com/vga-timing/640x480@60Hz
  // 50 MHz pixel frequency: http://tinyvga.com/vga-timing/800x600@72Hz

  /*
  640 x 480 @ 60 Hz
  with pixel frequency of 25.175 MHz

Scanline part	Pixels	Time [µs]
Visible area	640	25.422045680238
Front porch	16	0.63555114200596
Sync pulse	96	3.8133068520357
Back porch	48	1.9066534260179
Whole line	800	31.777557100298


Frame part	Lines	Time [ms]
Visible area	480	15.253227408143
Front porch	10	0.31777557100298
Sync pulse	2	0.063555114200596
Back porch	33	1.0486593843098
Whole frame	525	16.683217477656
   */

  // Standard 640 x 480 timing with 50 MHz clock
  val H_SYNC = 192
  val H_BACK_PORCH = 96
  val H_VISIBLE = 1280
  val H_FRONT_PORCH = 32
  val XMAX = H_SYNC + H_BACK_PORCH + H_VISIBLE + H_FRONT_PORCH

  val V_SYNC = 2
  val V_BACK_PORCH = 33
  val VISIBLE_LINES = 480
  val V_FRONT_PORCH = 10
  val YMAX = V_SYNC + V_BACK_PORCH + VISIBLE_LINES + V_FRONT_PORCH

  val (x, xDone) = Counter(true.B, XMAX-1)
  val (y, yDone) = Counter(xDone, YMAX-1)

  hSync := x >= 0.U && x < H_SYNC.U
  vSync := y >= 0.U && y < V_SYNC.U
  // Just a rectangle in the middle of the screen
  on := x >= 800.U&& x < 1200.U && y >= 200.U && y < 300.U

  io.uo_out := Cat(hSync, on, on, on, vSync, on, on, ledReg)
  io.uio_oe := 0.U
  io.uio_out := 0.U
}

object ChiselTop extends App {
  emitVerilog(new ChiselTop(), Array("--target-dir", "src"))
}