#!/usr/bin/env python3
#
# SPDX-FileCopyrightText: 2018 The Android Open Source Project
# SPDX-License-Identifier: Apache-2.0
#

"""
Tests gen_update_config.py.

Example:
    $ PYTHONPATH=$ANDROID_BUILD_TOP/build/make/tools/releasetools:$PYTHONPATH \\
        python3 -m unittest test_gen_update_config
"""

import os.path
import unittest
from gen_update_config import GenUpdateConfig


class GenUpdateConfigTest(unittest.TestCase): # pylint: disable=missing-docstring

    def test_ab_install_type_streaming(self):
        """tests if streaming property files' offset and size are generated properly"""
        config, package = self._generate_config()
        property_files = config['ab_config']['property_files']
        self.assertEqual(len(property_files), 6)
        with open(package, 'rb') as pkg_file:
            for prop in property_files:
                filename, offset, size = prop['filename'], prop['offset'], prop['size']
                pkg_file.seek(offset)
                raw_data = pkg_file.read(size)
                if filename in ['payload.bin', 'payload_metadata.bin']:
                    pass
                elif filename == 'payload_properties.txt':
                    pass
                elif filename == 'metadata':
                    self.assertEqual(raw_data.decode('ascii'), 'META-INF/COM/ANDROID/METADATA')
                else:
                    expected_data = filename.replace('.', '-').upper()
                    self.assertEqual(raw_data.decode('ascii'), expected_data)

    @staticmethod
    def _generate_config():
        """Generates JSON config from ota_002_package.zip."""
        ota_package = os.path.join(os.path.dirname(__file__),
                                   '../tests/res/raw/ota_002_package.zip')
        gen = GenUpdateConfig(ota_package,
                              'file:///foo.bar',
                              GenUpdateConfig.AB_INSTALL_TYPE_STREAMING,
                              True,  # ab_force_switch_slot
                              True)  # ab_verify_payload_metadata
        gen.run()
        return gen.config, ota_package
