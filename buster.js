var config = exports;

config['Browser Tests Development'] = {
    environment: 'browser',
    sources: [],
    tests: ["resources/js/dalap_core_browser_test.js"]
};

// config['Browser Tests Production'] = {
//     environment: 'browser',
//     sources: [],
//     tests: ["resources/js/dalap_core_browser_test_optimized.js"]
// };

config['Server Tests'] = {
     environment: 'node',
     sources: [],
     tests: ["resources/js/dalap_core_node_test.js"]
 };
