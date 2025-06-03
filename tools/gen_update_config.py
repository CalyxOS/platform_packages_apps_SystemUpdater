#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2018 The Android Open Source Project
# SPDX-License-Identifier: Apache-2.0
#

"""
Given a OTA package file, produces update config JSON file.

Example:
      $ PYTHONPATH=$ANDROID_BUILD_TOP/build/make/tools/releasetools:$PYTHONPATH \\
            packages/apps/SystemUpdater/tools/gen_update_config.py \\
                --ab_install_type=STREAMING \\
                ota-build-001.zip  \\
                my-config-001.json \\
                http://foo.bar/ota-builds/ota-build-001.zip
"""

import argparse
import json
import os.path
import sys
import zipfile

import ota_from_target_files  # pylint: disable=import-error
import ota_utils              # pylint: disable=import-error


class GenUpdateConfig(object):
    """
    A class that generates update configuration file from an OTA package.

    Currently supports only A/B (seamless) OTA packages.
    TODO: add non-A/B packages support.
    """

    def __init__(self,
                 package,
                 url,
                 changelog_url):
        self.package = package
        self.url = url
        self.changelog_url = changelog_url
        self._config = None

    @property
    def config(self):
        """Returns generated config object."""
        return self._config

    def run(self):
        """Generates config."""
        self._config = {
            '__': '*** Generated using tools/gen_update_config.py ***',
            'name': os.path.basename(self.package)[:-4],
            'url': self.url,
            'ab_config': self._gen_ab_config(),
            'changelog_url': self.changelog_url
        }

    def _gen_ab_config(self):
        """Builds config required for A/B update."""
        with zipfile.ZipFile(self.package, 'r') as package_zip:
            config = {
                'property_files': self._get_property_files(package_zip),
            }

        return config

    @staticmethod
    def _get_property_files(package_zip):
        """Constructs the property-files list for A/B streaming metadata."""

        ab_ota = ota_utils.AbOtaPropertyFiles()
        property_str = ab_ota.GetPropertyFilesString(package_zip, False)
        property_files = []
        for file in property_str.split(','):
            filename, offset, size = file.split(':')
            inner_file = {
                'filename': filename,
                'offset': int(offset),
                'size': int(size)
            }
            property_files.append(inner_file)

        return property_files

    def write(self, out):
        """Writes config to the output file."""
        with open(out, 'w') as out_file:
            json.dump(self.config, out_file, indent=4, separators=(',', ': '), sort_keys=True)


def main():  # pylint: disable=missing-docstring
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('package',
                        type=str,
                        help='OTA package zip file')
    parser.add_argument('out',
                        type=str,
                        help='Update configuration JSON file')
    parser.add_argument('url',
                        type=str,
                        help='OTA package download url')
    parser.add_argument('changelog_url',
                        type=str,
                        help='OTA package changelog url')
    args = parser.parse_args()

    if not args.out.endswith('.json'):
        print('out must be a json file')
        sys.exit(1)

    gen = GenUpdateConfig(
        package=args.package,
        url=args.url,
        changelog_url=args.changelog_url)
    gen.run()
    gen.write(args.out)
    print('Config is written to ' + args.out)


if __name__ == '__main__':
    main()
