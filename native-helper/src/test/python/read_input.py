#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys

"""Reads input lines and prints them until a blank line is entered"""

if __name__ == '__main__':
    while True:
        line = input()
        if line:
            print(line)
        else:
            break
    exit(0)
