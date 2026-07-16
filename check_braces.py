#!/usr/bin/env python3
import sys

def check_braces(filename):
    balance = 0
    with open(filename, 'r') as f:
        for line_num, line in enumerate(f, 1):
            for char in line:
                if char == '{':
                    balance += 1
                elif char == '}':
                    balance -= 1
            if 950 <= line_num <= 1150:
                print(f"Line {line_num}: Balance {balance} | {line.strip()}")
            if balance < 0:
                print(f"Balance negative at line {line_num}: {line.strip()}")
                return
    print(f"Final balance: {balance}")

if __name__ == "__main__":
    check_braces(sys.argv[1])
