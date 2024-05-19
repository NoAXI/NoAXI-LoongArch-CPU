simulator_path = ../test/soc-simulator
# simulator_path = ../test/soc-simulator-axi
myCPU_path = /mnt/f/CPU/lab_Loongarch/mycpu_env/myCPU
func_path = /mnt/f/CPU/lab_Loongarch/mycpu_env/func
generate_path = ./systemVerilog
EXP = 9

sim:
	@mill -i _.runMain Elaborate --target-dir $(generate_path)
	@echo -e "\e[32mGenerate Verilog completed. \e[0m"
	@cd $(generate_path) && cp Top.sv $(myCPU_path) 
	@cd $(simulator_path) && make clean
	@cd $(simulator_path) && make
	@echo -e "\e[32mlab$(EXP) Simulating... \e[0m"
	@cd $(simulator_path) && ./obj_dir/Vmycpu_top
#   @cd $(simulator_path) && ./obj_dir/Vmycpu_top -func -trace 10000000
	@echo -e "\e[32mlab$(EXP) Simulate completed. \e[0m"

generate:
	@mill -i _.runMain Elaborate --target-dir $(generate_path)
	@cd $(generate_path) && cp Top.sv $(myCPU_path)
	@echo -e "\e[32mGenerate Verilog completed. \e[0m"

wav:
	@cd $(simulator_path) && gtkwave trace.vcd debugwave.out.gtkw

# make set EXP=N
set:
	sed -i '0,/EXP = [0-9]\{1,2\}/s//EXP = $(EXP)/' Makefile
	@cd $(func_path) && make clean
	@cd $(func_path) && sed -i '0,/EXP = [0-9]\{1,2\}/s//EXP = $(EXP)/' Makefile
	@cd $(func_path) && make

compile:
	mill -i _.runMain Elaborate --target-dir $(generate_path)