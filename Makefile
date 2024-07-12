# simulator home
simulator_path = ./soc-simulator-axi

# verilog submit path
top_name = Top.sv
myCPU_path1 = /mnt/e/CPU/lab_Loongarch/mycpu_env/myCPU
myCPU_path2 = $(simulator_path)/myCPU
myCPU_path3 = /mnt/e/CPU/myCPU
func_path = /mnt/e/CPU/lab_Loongarch/mycpu_env/func

# generate path
generate_path = ./systemVerilog
EXP = 18

.PHONY: sim

sim:
	@mill -i _.runMain Elaborate --target-dir $(generate_path)
	@sed -i '/xilinx_single_port_ram_read_first.sv/d' $(generate_path)/Top.sv
	@sed -i '/xilinx_simple_dual_port_1_clock_ram_write_first.sv/d' $(generate_path)/Top.sv
	@sed -i '/xilinx_simple_dual_port_1_clock_ram_read_first.sv/d' $(generate_path)/Top.sv
	@echo -e "\e[32mGenerate Verilog completed. \e[0m"
	# @cd $(generate_path) && cp Top.sv $(myCPU_path1)
	# @cd $(generate_path) && cp Top.sv $(myCPU_path3)
	@cp $(generate_path)/$(top_name) $(myCPU_path2)/$(top_name)
	@cd $(simulator_path) && make clean
	@cd $(simulator_path) && make
	@echo -e "\e[32mlab$(EXP) Simulating... \e[0m"
	@cd $(simulator_path) && ./obj_dir/Vmycpu_top -perfdiff -uart -trace 1000000000 -perfonce
#	@cd $(simulator_path) && ./obj_dir/Vmycpu_top -perfdiff -uart -prog 1 
	@echo -e "\e[32mlab$(EXP) Simulate completed. \e[0m"

generate:
	@mill -i _.runMain Elaborate --target-dir $(generate_path)
	@cd $(generate_path) && cp Top.sv $(myCPU_path1)
#	@cd $(generate_path) && cp Top.sv $(myCPU_path2)
	@echo -e "\e[32mGenerate Verilog completed. \e[0m"

# make way x=N
wav:
	@cd $(simulator_path) && gtkwave trace-perf-$(x).vcd debugdiffwave.out.gtkw

# make set EXP=N
set:
	sed -i '0,/EXP = [0-9]\{1,2\}/s//EXP = $(EXP)/' Makefile
	@cd $(func_path) && make clean
	@cd $(func_path) && sed -i '0,/EXP = [0-9]\{1,2\}/s//EXP = $(EXP)/' Makefile
	@cd $(func_path) && make

compile:
	mill -i _.runMain Elaborate --target-dir $(generate_path)

count:
	@echo "count the lines"
	@find ./src -name "*.scala" | xargs wc -l