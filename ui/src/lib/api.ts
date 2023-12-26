const api_endpoint = "http://localhost:8080/api";

export function uploadFile(file: File, response_callback: (response: Response) => void, error_callback, overwrite: boolean = false) {
    let reader = new FileReader();
    let fileByteArray = [];
    reader.readAsArrayBuffer(file);
    reader.onloadend = (evt) => {
        if (evt.target?.readyState == FileReader.DONE) {
            let array = new Uint8Array(reader.result as ArrayBuffer);
            array.forEach((data) => fileByteArray.push(data));
        }
        let name = file.name;
        let bytes = new Uint8Array(fileByteArray);
        let url = `${api_endpoint}/upload?name=${name}&overwrite=${overwrite}`;

        fetch(url, {
            method: "POST",
            body: bytes,
        }).then(response => {
            response_callback(response);
        }).catch(error => {
            error_callback(error)
        });
    };
}

export function indexFile(res, callback, error_callback) {
    let url = `${api_endpoint}/index`;
    fetch(url, {
        method: "POST",
        body: JSON.stringify(res),
    }).then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        callback(response)
    }).catch(error => {
        error_callback(error)
    });
}

export function cancelFile(res, callback, error_callback) {
    let url = `${api_endpoint}/cancel`;
    fetch(url, {
        method: "POST",
        body: JSON.stringify(res),
    }).then(response => {
        callback(response)
    }).catch(error => {
        error_callback(error)
    });
}

export function loadFiles(fetch) {
    let url = `${api_endpoint}/load`;
    return fetch(url, {
        method: "GET",
    });
}

export function loadFile(filename, callback, error_callback) {
    let url = `${api_endpoint}/load-file?name=${filename}`;
    fetch(url, {
        method: "GET",
    }).then(response => {
        callback(response)
    }).catch(error => {
        error_callback(error)
    });
}

export function deleteFile(filename, callback, error_callback) {
    let url = `${api_endpoint}/delete-file?name=${filename}`;
    fetch(url, {
        method: "DELETE",
    }).then(response => {
        callback(response)
    }).catch(error => {
        error_callback(error)
    });
}


export function queryChunks(query_str, callback, error_callback) {
    let url = `${api_endpoint}/query?query=${query_str}`;
    fetch(url, {
        method: "GET",
    }).then(response => {
        callback(response)
    }).catch(error => {
        error_callback(error)
    });
}
