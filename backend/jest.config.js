module.exports = {
    clearMocks: true,
    preset: 'ts-jest',
    transform: {'^.+\\.ts?$': 'ts-jest'},
    testEnvironment: 'node',
    // testRegex: '/test/.*\\.(test|spec)?\\.(ts|tsx)$',
    moduleFileExtensions: ['ts', 'js'],
    // setupFilesAfterEnv: ['<rootDir>/test/singleton.ts'],
};