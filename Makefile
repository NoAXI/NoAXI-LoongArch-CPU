cp all:
	mill _.run
	# cd wave && ./sb.sh
	# cd wave && mv Top.sv mycpu_top.v
	# cd wave && cp mycpu_top.v /mnt/f/CPU/lab_Loongarch/mycpu_env/myCPU
	cd wave && cp Top.sv /mnt/f/CPU/lab_Loongarch/mycpu_env/myCPU

