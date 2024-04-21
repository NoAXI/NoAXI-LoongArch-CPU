sed -i "s/io_//g" Top.sv
sed -i "s/clock/clk/g" Top.sv
sed -i "s/reset/resetn/g" Top.sv
sed -i "s/module Top/module mycpu_top/g" Top.sv
sed -i "s/data_sram_waddr/data_sram_addr/g" Top.sv
sed -i "s/       (resetn)/       (reset)/g" Top.sv

if grep -q "ta	// src/main/scala/stages/Top.scala:31:16" Top.sv; then
# if grep -q "inst;	// src/main/scala/CPU/Top.scala:34:23" Top.sv; then
    echo "Modify Succseefully!!"
    sed -i "/bus;	\/\/ src\/main\/scala\/stages\/Top.scala:33:20/a   reg reset;\n always @(posedge clk) reset <= ~resetn;" Top.sv
    # sed -i "/inst;	\/\/ src\/main\/scala\/CPU\/Top.scala:34:23/a   reg reset;\n always @(posedge clk) reset <= ~resetn;" Top.sv
else
    echo "No Content!!"
fi  