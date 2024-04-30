simulator_path = ../test/soc-simulator
myCPU_path = /mnt/f/CPU/lab_Loongarch/mycpu_env/myCPU
func_path = /mnt/f/CPU/lab_Loongarch/mycpu_env/func
EXP = 11

sim:
	mill _.run
	@echo -e "\e[32mGenerate Verilog completed. \e[0m"
	@cd wave && cp Top.sv $(myCPU_path)
	@cd $(simulator_path) && make clean
	@cd $(simulator_path) && make
	@echo -e "\e[32mlab$(EXP) Simulating... \e[0m"
	@cd $(simulator_path) && ./obj_dir/Vmycpu_top
	@echo -e "\e[32mlab$(EXP) Simulate completed. \e[0m"

generate:
	mill _.run
	@cd wave && cp Top.sv $(myCPU_path)
	@echo -e "\e[32mGenerate Verilog completed. \e[0m"

wav:
	@cd $(simulator_path) && gtkwave trace.vcd debugwave.out.gtkw

# make set EXP=N
set:
	sed -i '0,/EXP = [0-9]\{1,2\}/s//EXP = $(EXP)/' Makefile
	@cd $(func_path) && make clean
	@cd $(func_path) && sed -i '0,/EXP = [0-9]\{1,2\}/s//EXP = $(EXP)/' Makefile
	@cd $(func_path) && make

	
