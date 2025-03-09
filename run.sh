# Remove old binary
rm -f file
rm -f omp

# Compilation with warning, PAPI and otimization flags
g++ -Wall -o file file.cpp -O2 -lpapi
g++ -Wall -o omp omp.cpp -O2 -lpapi -fopenmp

# Run tests set
echo "Running tests"
for attemp in {1..4}
do
    ./file "3" "1000" "20"  #still needs implementation so that file can receive arguments
done

for block_size in 256 512
do
    for attemp in {1..5}
    do
        ./omp "3" "1000" "$block_size" #still needs implementation so that file can receive arguments
    done
done