import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns


def first_plot(data_rust,data_parallel,data_single):
    # Set Seaborn style
    sns.set(style="whitegrid")

    # Filter for Function 1
    data_rust = data_rust.query("Function == 1")
    data_single = data_single.query("Function == 1")
    data_parallel = data_parallel.query("Function == 1")

    # Ensure Thread Number is properly set
    data_rust["Thread Number"] = 1  # Rust is single-threaded
    data_single["Thread Number"] = 1  # C++ single-threaded

    # Select only necessary columns
    columns_needed = ["Thread Number", "Col", "Time"]
    data_rust = data_rust[columns_needed]
    data_single = data_single[columns_needed]
    data_parallel = data_parallel[columns_needed]

    # Add an Implementation column
    data_rust["Implementation"] = "Rust"
    data_single["Implementation"] = "C++ (Single)"
    data_parallel["Implementation"] = "C++ (Parallel)"

    # Merge datasets
    data_combined = pd.concat([data_rust, data_single, data_parallel], ignore_index=True)

    # Ensure numerical columns are properly formatted
    data_combined["Time"] = pd.to_numeric(data_combined["Time"], errors="coerce")
    data_combined["Col"] = pd.to_numeric(data_combined["Col"], errors="coerce")
    data_combined["Thread Number"] = pd.to_numeric(data_combined["Thread Number"], errors="coerce")

    # **Plot: Execution Time vs Matrix Size for Different Threads**
    plt.figure(figsize=(10, 6))
    sns.lineplot(
        data=data_combined,
        x="Col", y="Time",
        hue="Thread Number",  # Different lines for each thread number
        style="Implementation",  # Different line styles for Rust, Single, Parallel
        markers=True
    )
    plt.title("Execution Time vs Matrix Size (Different Thread Counts) - Function 1")
    plt.ylabel("Execution Time (ms)")
    plt.xlabel("Matrix Size (Columns)")
    plt.legend(title="Threads")
    plt.grid(True, which="both", linestyle="--", linewidth=0.5)
    plt.show()

# def rust_vs_cpp_single(data_single, data_rust):
#     """Compares execution time between Rust and C++ (Single) for Function 1."""
#     
#     # Filter for Function 1
#     data_rust = data_rust.query("Function == 1").copy()
#     data_single = data_single.query("Function == 1").copy()
#
#     # Add an Implementation column for differentiation
#     data_rust["Implementation"] = "Rust"
#     data_single["Implementation"] = "C++ (Single)"
#
#     # Select necessary columns
#     columns_needed = ["Implementation", "Col", "Time"]
#     data_rust = data_rust[columns_needed]
#     data_single = data_single[columns_needed]
#
#     # Merge datasets
#     data_combined = pd.concat([data_rust, data_single], ignore_index=True)
#
#     # Convert columns to numeric for plotting
#     data_combined["Time"] = pd.to_numeric(data_combined["Time"], errors="coerce")
#     data_combined["Col"] = pd.to_numeric(data_combined["Col"], errors="coerce")
#
#     # **Plot: Rust vs C++ (Single) Execution Time**
#     plt.figure(figsize=(10, 6))
#     sns.lineplot(
#         data=data_combined,
#         x="Col", y="Time",
#         hue="Implementation",
#         markers=True
#     )
#     plt.title("Execution Time Comparison: Rust vs C++ (Single)")
#     plt.ylabel("Execution Time (ms)")
#     plt.xlabel("Matrix Size (Columns)")
#     plt.legend(title="Implementation")
#     plt.grid(True, linestyle="--", linewidth=0.5)
#     plt.show()

def single_vs_multi_core(data_single, data_parallel):
    # Set Seaborn style
    sns.set(style="whitegrid")

    # Filter for Function 1
    data_single = data_single.query("Function == 2")
    data_parallel = data_parallel.query("Function == 2")

    # Ensure Thread Number is properly set
    data_single["Thread Number"] = 1  # C++ single-threaded

    # Select only necessary columns
    columns_needed = ["Thread Number", "Col", "Time"]
    data_single = data_single[columns_needed]
    data_parallel = data_parallel[columns_needed]

    # Add an Implementation column
    data_single["Implementation"] = "C++ (Single)"
    data_parallel["Implementation"] = "C++ (Parallel)"

    # Merge datasets
    data_combined = pd.concat([ data_single, data_parallel], ignore_index=True)

    # Ensure numerical columns are properly formatted
    data_combined["Time"] = pd.to_numeric(data_combined["Time"], errors="coerce")
    data_combined["Col"] = pd.to_numeric(data_combined["Col"], errors="coerce")
    data_combined["Thread Number"] = pd.to_numeric(data_combined["Thread Number"], errors="coerce")

    # **Plot: Execution Time vs Matrix Size for Different Threads**
    plt.figure(figsize=(10, 6))
    sns.lineplot(
        data=data_combined,
        x="Col", y="Time",
        hue="Thread Number",  # Different lines for each thread number
        style="Implementation",  # Different line styles for Rust, Single, Parallel
        markers=True
    )
    plt.title("Execution Time vs Matrix Size (Different Thread Counts), For function 2")
    plt.ylabel("Execution Time (ms)")
    plt.xlabel("Matrix Size (Columns)")
    plt.legend(title="Threads")
    plt.grid(True, which="both", linestyle="--", linewidth=0.5)
    plt.show()



def single_vs_line_function(data_single):
    """Compares execution time for Single vs Line function in data_single."""

    # Set Seaborn style
    sns.set(style="whitegrid")

    # Filter only relevant functions (assuming function names are 'single' and 'line')
    data_filtered = data_single.query("Function in [1, 2]").copy()

    # Select necessary columns
    columns_needed = ["Function", "Col", "Time"]
    data_filtered = data_filtered[columns_needed]

    # Ensure numerical columns are properly formatted
    data_filtered["Time"] = pd.to_numeric(data_filtered["Time"], errors="coerce")
    data_filtered["Col"] = pd.to_numeric(data_filtered["Col"], errors="coerce")

    # Rename function labels for clarity
    function_labels = {1: "Single", 2: "Line"}
    data_filtered["Function"] = data_filtered["Function"].map(function_labels)

    # **Plot: Execution Time vs Matrix Size (Single vs Line)**
    plt.figure(figsize=(10, 6))
    sns.lineplot(
        data=data_filtered,
        x="Col", y="Time",
        hue="Function",  # Different lines for "Single" vs "Line"
        markers=True
    )
    plt.title("Execution Time Comparison: Single vs Line Function (C++ Single)")
    plt.ylabel("Execution Time (ms)")
    plt.xlabel("Matrix Size (Columns)")
    plt.legend(title="Function Type")
    plt.ylim(0,150)
    plt.xlim(0,3200)
    plt.grid(True, linestyle="--", linewidth=0.5)
    plt.show()

def block_matrix_single( data_parallel):
    """Compares execution time based on Block Size and Matrix Size for Function 3 (Single vs Parallel)."""

    # Set Seaborn style
    sns.set(style="whitegrid")

    # Filter for Function 3
    data_parallel = data_parallel.query("Function == 3").copy()


    # Select necessary columns
    columns_needed = ["Col", "Block Size", "Time"]
    data_parallel = data_parallel[columns_needed]

    # Ensure numerical columns are properly formatted
    data_parallel["Time"] = pd.to_numeric(data_parallel["Time"], errors="coerce")
    data_parallel["Col"] = pd.to_numeric(data_parallel["Col"], errors="coerce")
    data_parallel["Block Size"] = pd.to_numeric(data_parallel["Block Size"], errors="coerce")

    # **Plot: Execution Time vs Block Size & Matrix Size**
    plt.figure(figsize=(10, 6))
    sns.lineplot(
        data=data_parallel.dropna(subset=["Block Size"]),  # Remove rows where Block Size is NaN
        x="Block Size", y="Time",
        hue="Col",  # Different lines for different matrix sizes
        markers=True
    )
    plt.title("Execution Time vs Block Size & Matrix Size - Function 3 - Single")
    plt.ylabel("Execution Time (ms)")
    plt.xlabel("Block Size")
    plt.legend(title="Matrix Size (Columns)")
    plt.grid(True, linestyle="--", linewidth=0.5)
    plt.show()



def block_matrix_parallel_threads(data_parallel):
    """Compares execution time based on Block Size, Matrix Size, and Thread Count for Function 3 (Parallel)."""

    # Set Seaborn style
    sns.set(style="whitegrid")

    # Filter for Function 3
    data_parallel = data_parallel.query("Function == 3").copy()

    # Ensure numerical columns are properly formatted
    data_parallel["Time"] = pd.to_numeric(data_parallel["Time"], errors="coerce")
    data_parallel["Col"] = pd.to_numeric(data_parallel["Col"], errors="coerce")
    data_parallel["Block Size"] = pd.to_numeric(data_parallel["Block Size"], errors="coerce")
    data_parallel["Thread Number"] = pd.to_numeric(data_parallel["Thread Number"], errors="coerce")

    # Select unique thread numbers for separate plots
    unique_threads = sorted(data_parallel["Thread Number"].unique())[:3]  # Pick first 4 unique thread numbers

    # **Plot: 4 Subplots for Different Thread Numbers**
    fig, axes = plt.subplots(1, 3, figsize=(18, 6), sharex=True, sharey=True)
    axes = axes.flatten()  # Convert to a 1D array for easier iteration

    for i, thread in enumerate(unique_threads):
        ax = axes[i]

        # Filter data for the current thread number
        data_filtered = data_parallel[data_parallel["Thread Number"] == thread]

        # Create line plot
        sns.lineplot(
            data=data_filtered.dropna(subset=["Block Size"]),  # Remove rows where Block Size is NaN
            x="Block Size", y="Time",
            hue="Col",  # Different lines for different matrix sizes
            markers=True, ax=ax
        )

        ax.set_title(f"Threads: {thread}")
        ax.set_xlabel("Block Size")
        ax.set_ylabel("Execution Time (ms)")
        ax.legend(title="Matrix Size (Columns)")
        ax.grid(True, linestyle="--", linewidth=0.5)

    # Adjust layout
    plt.suptitle("Execution Time vs Block Size & Matrix Size - Function 3 (Parallel)")
    plt.tight_layout(rect=(0, 0, 1, 0.96))
    plt.show()


def cache_misses_single(data_single, function_id):
    """Analyzes L1 and L2 cache misses for a specific function in single-threaded execution.
       - If function == 3, it considers Block Size as well. """

    # Set Seaborn style
    sns.set(style="whitegrid")

    # Filter for the selected function
    data_filtered = data_single.query(f"Function == {function_id}").copy()

    # If Function == 3, include Block Size in analysis
    if function_id == 3:
        columns_needed = ["Col", "Block Size", "L1", "L2"]
    else:
        columns_needed = ["Col", "L1", "L2"]

    data_filtered = data_filtered[columns_needed]

    # Ensure numerical columns are properly formatted
    data_filtered["Col"] = pd.to_numeric(data_filtered["Col"], errors="coerce")
    data_filtered["L1"] = pd.to_numeric(data_filtered["L1"], errors="coerce")
    data_filtered["L2"] = pd.to_numeric(data_filtered["L2"], errors="coerce")

    if function_id == 3:
        data_filtered["Block Size"] = pd.to_numeric(data_filtered["Block Size"], errors="coerce")

    # **Plot: L1 & L2 Cache Misses vs Matrix Size**
    plt.figure(figsize=(10, 6))

    if function_id == 3:
        # Differentiate by Block Size
        sns.lineplot(data=data_filtered, x="Col", y="L1", hue="Block Size", 
                     marker="o", palette="coolwarm", linestyle="--")  # Dotted for L1
        sns.lineplot(data=data_filtered, x="Col", y="L2", hue="Block Size", 
                     marker="s", palette="coolwarm", linestyle="-")   # Solid for L2
        plt.legend(title="Block Size")
    else:
        # Standard Single-Core Cache Miss Plot
        sns.lineplot(data=data_filtered, x="Col", y="L1", label="L1 Cache Misses", 
                     marker="o", color="b", linestyle="--")  # Dotted for L1
        sns.lineplot(data=data_filtered, x="Col", y="L2", label="L2 Cache Misses", 
                     marker="s", color="r", linestyle="-")   # Solid for L2
        plt.legend(title="Cache Level")

    # Titles and Labels
    plt.title(f"Cache Misses vs Matrix Size (Single-Core) - Function {function_id}")
    plt.ylabel("Cache Misses")
    plt.xlabel("Matrix Size (Columns)")
    plt.grid(True, linestyle="--", linewidth=0.5)
    plt.show()



def cache_misses_parallel(data_single, function_id,thread_id):
    """Analyzes L1 and L2 cache misses for a specific function in single-threaded execution.
       - If function == 3, it considers Block Size as well. """

    # Set Seaborn style
    sns.set(style="whitegrid")

    # Filter for the selected function
    data_filtered = data_single.query(f"Function == {function_id}").copy()
    data_filtered = data_single[data_single["Thread Number"] == thread_id].copy()

    # If Function == 3, include Block Size in analysis
    if function_id == 3:
        columns_needed = ["Col", "Block Size", "L1", "L2"]
    else:
        columns_needed = ["Col", "L1", "L2"]

    data_filtered = data_filtered[columns_needed]

    # Ensure numerical columns are properly formatted
    data_filtered["Col"] = pd.to_numeric(data_filtered["Col"], errors="coerce")
    data_filtered["L1"] = pd.to_numeric(data_filtered["L1"], errors="coerce")
    data_filtered["L2"] = pd.to_numeric(data_filtered["L2"], errors="coerce")

    if function_id == 3:
        data_filtered["Block Size"] = pd.to_numeric(data_filtered["Block Size"], errors="coerce")

    # **Plot: L1 & L2 Cache Misses vs Matrix Size**
    plt.figure(figsize=(10, 6))

    if function_id == 3:
        # Differentiate by Block Size
        sns.lineplot(data=data_filtered, x="Col", y="L1", hue="Block Size", 
                     marker="o", palette="coolwarm", linestyle="--",ci=None)  # Dotted for L1
        sns.lineplot(data=data_filtered, x="Col", y="L2", hue="Block Size", 
                     marker="s", palette="coolwarm", linestyle="-",ci=None)   # Solid for L2
        plt.legend(title="Block Size")
    else:
        # Standard Single-Core Cache Miss Plot
        sns.lineplot(data=data_filtered, x="Col", y="L1", label="L1 Cache Misses", 
                     marker="o", color="b", linestyle="--",ci=None)  # Dotted for L1
        sns.lineplot(data=data_filtered, x="Col", y="L2", label="L2 Cache Misses", 
                     marker="s", color="r", linestyle="-",ci=None)   # Solid for L2
        plt.legend(title="Cache Level")

    # Titles and Labels
    plt.title(f"Cache Misses vs Matrix Size (Multi-Core) - Function {function_id} - Nº of Threads {thread_id}")
    plt.ylabel("Cache Misses")
    plt.xlabel("Matrix Size (Columns)")
    plt.grid(True, linestyle="--", linewidth=0.5)
    plt.show()

if __name__ == "__main__":
# Load data
    data_rust = pd.read_csv("data_rust.csv")
    data_single = pd.read_csv("data_single.csv")
    data_parallel = pd.read_csv("data_parallel.csv")
    first_plot(data_single,data_parallel,data_rust)
    # rust_vs_cpp_single(data_single, data_rust)
    single_vs_multi_core(data_single, data_parallel)
    single_vs_line_function(data_single)
    block_matrix_single(data_single)
    block_matrix_parallel_threads(data_parallel)
    for i in [1,2,3]: 
        cache_misses_single(data_single,i)
    for i in [1,2,3]: 
        for j in [1,8,16]:
            cache_misses_parallel(data_parallel, i,j)
