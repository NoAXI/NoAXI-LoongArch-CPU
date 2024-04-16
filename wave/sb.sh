sed -i "s/io_//g" Top.sv
sed -i "s/clock/clk/g" Top.sv
sed -i "s/reset/resetn/g" Top.sv
sed -i "s/       (resetn)/       (reset)/g" Top.sv

if grep -q "ta	// src/main/scala/stages/Top.scala:31:16" Top.sv; then
    echo "Modify Succseefully!!"
    sed -i "/bus;	\/\/ src\/main\/scala\/stages\/Top.scala:33:20/a reg reset;always @(posedge clk) reset <= ~resetn;" Top.sv
else
    echo "No file"
fi