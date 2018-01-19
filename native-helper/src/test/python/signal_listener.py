#!/usr/bin/env python3

from __future__ import print_function
import signal
import os
import sys


def main():
    from argparse import ArgumentParser
    swallowed = [signal.SIGINT]
    parser = ArgumentParser()
    parser.add_argument("--swallow-sigint", help="swallow SIGINT", action='store_true')
    parser.add_argument("--swallow-sigterm", help="swallow SIGTERM", action='store_true')
    parser.add_argument("--pidfile", help="print PID to this file on start")
    parser.add_argument("--quiet", help="do not print anything to console", action='store_true')
    args = parser.parse_args()
    if args.swallow_sigint:
        swallowed.append(signal.SIGINT)
    if args.swallow_sigterm:
        swallowed.append(signal.SIGTERM)
    if args.pidfile:
        with open(args.pidfile, 'w') as ofile:
            print(os.getpid(), file=ofile)
    stdout = sys.stdout if not args.quiet else open("/dev/null", "w")

    def signal_handler(signalnum, frame):
        print("signal", signalnum, file=stdout)
    for swallowable in swallowed:
        signal.signal(swallowable, signal_handler)
    while True:
        signal.pause()


if __name__ == '__main__':
    main()
