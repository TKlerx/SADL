#!/bin/bash
./smac --shared-model-mode true --scenario-file pdtta/pdtta-conf.txt --seed 453069944 &
./smac --shared-model-mode true --scenario-file pdtta/pdtta-conf.txt --seed 754598994 & 
./smac --shared-model-mode true --scenario-file pdtta/pdtta-conf.txt --seed 137932278 &
./smac --shared-model-mode true --scenario-file pdtta/pdtta-conf.txt --seed 919687770 &

#8 runs of SMAC are started and running in the background, now we wait for all processes to complete.
wait		
