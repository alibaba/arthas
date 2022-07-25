/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
// jest.config.js
const { pathsToModuleNameMapper } = require('ts-jest')
// In the following statement, replace `./tsconfig` with the path to your `tsconfig` file
// which contains the path mapping (ie the `compilerOptions.paths` option):
const { compilerOptions } = require("./tsconfig.json")

module.exports = {
  // preset: 'ts-jest',
  testEnvironment: 'node',
  preset: 'ts-jest/presets/default-esm', // or other ESM presets
  globals: {
    'ts-jest': {
      useESM: true,
    },
  },
  // moduleNameMapper: { '^@/(.*)$': '<rootDir>/src/$1' },
  moduleNameMapper: pathsToModuleNameMapper(compilerOptions.paths , { prefix: '<rootDir>/' } ),
};