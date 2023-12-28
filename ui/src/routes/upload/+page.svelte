<script lang="ts">
    import {Button, Dropzone, Checkbox, Modal, Label, Textarea, Spinner} from "flowbite-svelte";
    import {cancelFile, indexFile, uploadFile} from "$lib/api.js";
    import ButtonSecondary from "$lib/ButtonSecondary.svelte";

    let loading = false;
    let overwrite = false;
    let open_modal = true;
    let est_pricing = 0.0;
    let title = "";
    let mimetype = "";
    let text = "this is an endlessly long text that does stuff ya know?";
    let upload_results = [];

    $: if (upload_results.length > 0) {
        open_modal = true;
        loading = false;
        let res = upload_results[upload_results.length - 1];
        title = res.name;
        mimetype = res.mime_type;
        est_pricing = res.estm_pricing;
        text = res.text;
    } else {
        open_modal = false;
    }

    let value = [];
    const dropHandle = (event) => {
        value = [];
        event.preventDefault();
        if (event.dataTransfer.items) {
            [...event.dataTransfer.items].forEach((item, i) => {
                if (item.kind === 'file') {
                    const file = item.getAsFile();
                    value.push(file.name);
                    value = value;
                }
            });
        } else {
            [...event.dataTransfer.files].forEach((file, i) => {
                value = file.name;
            });
        }
    };

    const handleChange = (event) => {
        const files = event.target.files;
        value = []
        for (const file of files) {
            value.push(file);
        }
    };

    const showFiles = (files) => {
        if (files.length === 1) return files[0].name;
        let concat = '';
        files.map((file) => {
            concat += file.name;
            concat += ',';
            concat += ' ';
        });

        if (concat.length > 100) concat = concat.slice(0, 100);
        concat += '...';
        return concat;
    };

    let callback = (response: Response) => {
        if (response.status === 201) {
            response.json().then((data) => {
                upload_results.push(data)
                upload_results = upload_results;
            })
        } else {
            response.json().then((data) => {
                alert("Upload failed with msg: " + data.message)
            })
        }
    }
    let on_error = (error: Error) => {
        alert("Upload failed with msg: " + error.message)
        open_modal = false;
        upload_results = upload_results;
        loading = false;
    }

    let on_success = (response: Response) => {
        if (response.status === 201) {
            response.json().then((data) => {
                alert("Successful: " + data.message)
                open_modal = false;
                upload_results = upload_results;
                loading = false;
            })
        } else {
            response.json().then((data) => {
                alert("Failed with message: " + data.message)
                open_modal = false;
                upload_results = upload_results;
                loading = false;
            })
        }
    }

    function upload() {
        for (const file of value) {
            uploadFile(file, callback, on_error, overwrite)
        }
    }

    function index() {
        let res = upload_results.pop()
        indexFile(res, on_success, on_error)
    }

    function cancel() {
        let res = upload_results.pop()
        cancelFile(res, on_success, on_error)
    }

</script>

<div class="w-1/2 m-auto pt-5">
    <h1 class="pb-2 font-bold text-2xl">
        Upload File here
    </h1>

    <div class="pb-5">
        <div class="pb-3">
            <Dropzone
                    id="dropzone"
                    on:drop={dropHandle}
                    on:dragover={(event) => {
                        event.preventDefault();
                    }}
                    on:change={handleChange}
                    multiple={true}
                    accept=".pdf, .txt">
                <svg aria-hidden="true" class="mb-3 w-10 h-10 text-gray-400" fill="none" stroke="currentColor"
                     viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
                </svg>
                {#if value.length === 0}
                    <p class="mb-2 text-sm text-gray-500 dark:text-gray-400"><span
                            class="font-semibold">Click to upload</span> or drag and drop</p>
                    <p class="text-xs text-gray-500 dark:text-gray-400">SVG, PNG, JPG or GIF (MAX. 800x400px)</p>
                {:else}
                    <p>{showFiles(value)}</p>
                {/if}
            </Dropzone>
        </div>

        <div class="block py-2 px-1 mb-3 border border-gray-300 rounded-xl w-fit">
            <Checkbox bind:checked={overwrite}>Overwrite</Checkbox>
        </div>

        <Button class="shadow-xl" on:click={upload}>Upload</Button>
    </div>


    <Modal title="Index Preview" bind:open={open_modal}>
        {#if loading}
            <div class="z-10">
                <Spinner/>
            </div>
        {/if}
        <div class="p-5 overflow-scroll h-full">
            <div class="pb-3">
                <p class="text-sm text-gray-500 dark:text-gray-400">Title: {title}</p>
            </div>
            <div class="pb-3">
                <p class="text-sm text-gray-500 dark:text-gray-400">Estimated pricing: {est_pricing} USD (Based on
                    OpenAI API)</p>
            </div>
            <div class="pb-3">
                <p class="text-sm text-gray-500 dark:text-gray-400">Mine-Type: {mimetype}</p>
            </div>
            <div class="pb-3">
                <p class="text-sm text-gray-500 dark:text-gray-400">Text-Preview:</p>
                <p class="text-sm text-gray-500 dark:text-gray-400 h-[426px] overflow-scroll border rounded-xl px-1 py-2 whitespace-pre-line">{text}</p>
            </div>
            <div class="block">
                <Button on:click={index}>Index</Button>
                <ButtonSecondary on:click={cancel}>Cancel</ButtonSecondary>
                <!--                <Button class="hover:bg-secondary-700 hover:text-white border border-secondary-700 bg-white text-secondary-700" on:click={cancel}>Cancel</Button> -->
            </div>
        </div>
    </Modal>

</div>
