# import matplotlib as plo
import csv
import os
import subprocess


def compile_cpp(source_file):
    try:
        if(source_file == "omp"):
            os.system(f"g++  -o omp omp.cpp -O2 -lpapi -fopenmp")
            return "omp"
        elif(source_file=="file"):
            os.system(f"g++ -Wall -o file file.cpp -O2 -lpapi")
            return "file"
        elif source_file == "file_rust":
            os.system("cd Rust && cargo build --release && ls && mv target/release/Resu ../file_rust")
            return "file_rust"
        

    except FileNotFoundError:
        print("Error: g++ is not installed or not found in PATH.")
        return None

def run_executable(executable):
    if os.path.exists(executable):
        if (exe == "omp"):
            with open('data_parallel.csv','a',newline='') as file:
                writer = csv.writer(file)
                writer.writerow(['Function','Line','Col','Block Size','Time','L1','L2'])
                for j in range(600,3001,400):
                    result = subprocess.run(["./" + executable, str(1), str(j)], capture_output=True, text=True)
                    values = [float(x) for x in result.stdout.strip().split()]        
                    writer.writerow([1,j,j,'None',values[0],values[1],values[2]])

                for j in range(600,3001,400):
                    result = subprocess.run(["./" + executable, str(2), str(j)], capture_output=True, text=True)
                    values = [float(x) for x in result.stdout.strip().split()]        
                    writer.writerow([2,j,j,'None',values[0],values[1],values[2]])
                for j in range(4096,10241,2048):
                    result = subprocess.run(["./" + executable, str(2), str(j)], capture_output=True, text=True)
                    values = [float(x) for x in result.stdout.strip().split()]        
                    writer.writerow([2,j,j,'None',values[0],values[1],values[2]])
                k = 2
                for j in range(4096,10241,2048):
                    while k < 4096:
                        k *= 2
                        result = subprocess.run(["./" + executable, str(3), str(j),str(k)], capture_output=True, text=True)
                        values = [float(x) for x in result.stdout.strip().split()]        
                        writer.writerow([3,j,j,k,values[0],values[1],values[2]])
        elif (exe =="file"):
            with open('data_single.csv','a',newline='') as file:
                writer = csv.writer(file)
                writer.writerow(['Function','Line','Col','Block Size','Time','L1','L2'])
                for j in range(600,3001,400):
                    result = subprocess.run(["./" + executable, str(1), str(j)], capture_output=True, text=True)
                    values = [float(x) for x in result.stdout.strip().split()]        
                    writer.writerow([1,j,j,'None',values[0],values[1],values[2]])

                for j in range(600,3001,400):
                    result = subprocess.run(["./" + executable, str(2), str(j)], capture_output=True, text=True)
                    values = [float(x) for x in result.stdout.strip().split()]        
                    writer.writerow([2,j,j,'None',values[0],values[1],values[2]])
                for j in range(4096,10241,2048):
                    result = subprocess.run(["./" + executable, str(2), str(j)], capture_output=True, text=True)
                    values = [float(x) for x in result.stdout.strip().split()]        
                    writer.writerow([2,j,j,'None',values[0],values[1],values[2]])
                k = 2
                for j in range(4096,10241,2048):
                    while k < 4096:
                        k *= 2
                        result = subprocess.run(["./" + executable, str(3), str(j),str(k)], capture_output=True, text=True)
                        values = [float(x) for x in result.stdout.strip().split()]        
                        writer.writerow([3,j,j,k,values[0],values[1],values[2]])
        elif(exe=="file_rust"):
            with open('data_rust.csv','a',newline='') as file:
                writer = csv.writer(file)
                writer.writerow(['Function','Line','Col','Time'])
                for j in range(600,3001,400):
                    result = subprocess.run(["./" + executable, str(2), str(j)], capture_output=True, text=True)
                    values = [float(x) for x in result.stdout.strip().split()]        
                    writer.writerow([1,j,j,values[0]])


    else:
        print("Error: Executable not found.")

if __name__ == "__main__":
    file = input("Enter either omp or file or file_rust:")
    output_file = "program"
    exe = compile_cpp(file)
    if exe:
        run_executable(exe)


