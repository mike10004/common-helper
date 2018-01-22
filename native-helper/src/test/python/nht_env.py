#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import sys
import os

"""Print values of environment variables"""

if __name__ == '__main__':
    from argparse import ArgumentParser
    parser = ArgumentParser()
    parser.add_argument("varnames", metavar="VAR", help="environment variable names to print", nargs='+')
    parser.add_argument("--skip_undefined", action="store_true", default="False")
    args = parser.parse_args()
    for varname in args.varnames:
        value = os.getenv(varname)
        if value is None:
            if not args.skip_undefined:
                print("unknown variable name", varname, file=sys.stderr)
                exit(1)
        else:
            print(value)
    exit(0)
