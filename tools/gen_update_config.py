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
import ota_metadata_pb2       # pylint: disable=import-error
import ota_utils              # pylint: disable=import-error


class GenUpdateConfig(object):
    """
    A class that generates update configuration file from an OTA package.

    Currently supports only A/B (seamless) OTA packages.
    TODO: add non-A/B packages support.
    """

    def __init__(self,
                 packages,
                 changelog_url):
        self.packages = packages
        self.changelog_url = changelog_url
        self.streaming_required = (
            # payload.bin and payload_properties.txt must exist.
            'payload.bin',
            'payload_properties.txt',
        )
        self.streaming_optional = (
            # care_map.txt is available only if dm-verity is enabled.
            'care_map.txt',
            # compatibility.zip is available only if target supports Treble.
            'compatibility.zip',
        )
        self._config = None

    @property
    def config(self):
        """Returns generated config object."""
        return self._config

    def run(self):
        """Generates config."""
        self._config = {
            '__': '*** Generated using tools/gen_update_config.py ***',
            'changelog_url': self.changelog_url
        }
        self._update_config_from_packages()

    def _update_config_from_packages(self):
        """Builds config required for A/B update."""
        num_full_otas = 0
        zips = []
        for package in self.packages:
            with zipfile.ZipFile(package, 'r') as package_zip:
                metadata = ota_metadata_pb2.OtaMetadata()
                metadata.ParseFromString(package_zip.read(ota_utils.METADATA_PROTO_NAME))
                pre_update = metadata.precondition
                update = metadata.postcondition
                is_full_ota = not pre_update.build_incremental

                this_zip = {}

                if is_full_ota:
                    if num_full_otas > 0:
                        raise RuntimeError("Expected at most a single full ota zip!")
                    num_full_otas += 1
                else:
                    this_zip['from'] = pre_update.build_incremental

                # Get overall build information from the first zip.
                # If the first zip isn't a full OTA, replace the build info later with that
                # of a full OTA, if any.
                if is_full_ota or not "build_date_utc" in self._config:
                    # TODO: Check if any of this info differs among provided OTAs to prevent
                    #       silly mistakes?
                    self._config.update({
                        "build_date_utc": update.timestamp,
                        "build_number": update.build_incremental,
                        "sdk_level": update.sdk_level,
                        "security_patch_level": update.security_patch_level,
                    })

                this_zip.update({
                    'filename': os.path.basename(package),
                    'property_files': self._get_property_files(package_zip),
                })
                zips.append(this_zip)
        self._config["zips"] = zips

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
    parser.add_argument('out',
                        type=str,
                        help='Update configuration JSON file')
    parser.add_argument('changelog_url',
                        type=str,
                        help='OTA package changelog url')
    parser.add_argument('package',
                        type=str,
                        help='OTA package zip file',
                        nargs='+')
    args = parser.parse_args()

    if not args.out.endswith('.json'):
        print('out must be a json file')
        sys.exit(1)

    gen = GenUpdateConfig(
        packages=args.package,
        changelog_url=args.changelog_url)
    gen.run()
    gen.write(args.out)
    print('Config is written to ' + args.out)


if __name__ == '__main__':
    main()
