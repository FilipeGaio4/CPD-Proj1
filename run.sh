# Remove old binary
rm -f fabiosa

# Compilation with warning, PAPI and otimization flags
g++ -Wall -o fabiosa fabiosa.cpp -O2 -lpapi

# Run tests set
echo "Algorithm 3 - Matrix 10240"
for attemp in {1..4}
do
    ./fabiosa "3" "1000" "20" "data/data.txt"
done

for block_size in 256 512
do
    for attemp in {1..5}
    do
        ./fabiosa "3" "10240" "$block_size" "data/data.txt"
    done
done