import {loadFiles} from "$lib/api.js";


/** @type {import('./$types').PageLoad} */
export async function load({fetch, params}) {
    let response = await loadFiles(fetch)
    if (response.ok) {
        return {
            data: await response.json(),
            loading: false,
        }
    }
}