use std::alloc::{alloc, dealloc, Layout};
use std::ptr;
use std::{cmp::min, io, time::Instant, vec};

fn on_mult(line: u32, col: u32) {
    let mut _st: [char; 100];

    let mut temp: f64;

    let mut pha = vec![0.0; (line * col) as usize];
    let mut phb = vec![0.0; (line * col) as usize];
    let mut phc = vec![0.0; (line * col) as usize];

    for i in 0..line {
        for j in 0..line {
            let idx = (i * line + j) as usize;
            pha[idx] = 1.0;
        }
    }

    for i in 0..col {
        for j in 0..col {
            let idx = (i * col + j) as usize;
            phb[idx] = (i + 1) as f64;
        }
    }
    let start = Instant::now();
    for i in 0..line {
        for j in 0..col {
            temp = 0.0;
            for k in 0..line {
                temp += pha[(i * line + k) as usize] * phb[(k * col + j) as usize];
            }
            phc[(i * line + j) as usize] = temp;
        }
    }
    let result_time = start.elapsed();

    println!("Time in milliseconds: {}", result_time.as_millis());

    println!("Result Matrix is:");
    for _i in 0..1 {
        for j in 0..(min(10, col)) {
            println!("{}", phc[j as usize]);
        }
    }
}

fn on_mult_unsafe(line: u32, col: u32) {
    let size = (line * col) as usize;

    // Define the memory layout
    let layout = Layout::array::<f64>(size).unwrap();
    let mut _st: [char; 100];

    let mut temp: f64;

    let pha = unsafe { alloc(layout) as *mut f64 };
    let phb = unsafe { alloc(layout) as *mut f64 };
    let phc = unsafe { alloc(layout) as *mut f64 };

    unsafe {
        for i in 0..line {
            for j in 0..col {
                let idx = (i * col + j) as usize;
                ptr::write(pha.add(idx), 1.0);
            }
        }

        for i in 0..col {
            for j in 0..col {
                let idx = (i * col + j) as usize;
                ptr::write(phb.add(idx), (i + 1) as f64);
            }
        }
        let start = Instant::now();
        for i in 0..line {
            for j in 0..col {
                temp = 0.0;
                for k in 0..line {
                    temp += *pha.add((i * col + k) as usize) * *phb.add((k * col + j) as usize);
                }
                ptr::write(phc.add((i * col + j) as usize), temp);
            }
        }
        let result_time = start.elapsed();

        println!("Time in milliseconds: {}", result_time.as_millis());

        println!("Result Matrix is:");
        for _i in 0..1 {
            for j in 0..(min(10, col)) {
                println!("{}", *phc.add(j as usize));
            }
        }
    }
}

fn main() {
    loop {
        println!("1. Multiplication");
        println!("2. Multiplication Unsafe");
        println!("3. Line Multiplication");
        println!("4. Block Multiplication");

        let mut choice = String::new();

        io::stdin().read_line(&mut choice).expect("Error");

        let choice: u8 = match choice.trim().parse() {
            Ok(num) => num,
            Err(_) => break,
        };

        let mut choice2 = String::new();

        println!("Dimensions: lins=cols ? ");

        io::stdin().read_line(&mut choice2).expect("Error");

        let choice2: u32 = match choice2.trim().parse() {
            Ok(size) => size,
            Err(_) => panic!("Error"),
        };

        match choice {
            1 => on_mult(choice2, choice2),
            2 => on_mult_unsafe(choice2, choice2),
            _ => break,
        }
    }
}