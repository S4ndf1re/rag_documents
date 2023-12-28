<script lang="ts">
    import {deleteFile, loadFile, loadFiles} from "$lib/api.js";
    import {
        Table,
        TableBody,
        TableBodyCell,
        TableBodyRow,
        TableHead,
        TableHeadCell,
        Button, Modal
    } from 'flowbite-svelte';
    import {FolderOpenOutline} from "flowbite-svelte-icons";
    import ButtonSecondary from "$lib/ButtonSecondary.svelte";
    import {load} from "./+page";
    import {invalidate, invalidateAll} from "$app/navigation";


    let loading = true;
    let open_modal = false;

    /** @type {import('./$types').PageData} */
    export let data;

    let files = {};
    $: files = data.data;
    $: loading = data.loading;


    let title = "";
    let mimetype = "";
    let text = "";

    let closeModal = () => {
        open_modal = false;
    }


    let delete_success_callback = (response) => {
        response.json().then((data) => {
            alert(data.message)
            open_modal = false;
            loading = true;
            files = {};
            invalidate("/api/load-files");
        });
    }

    let delete_error_callback = (response) => {
        response.json().then((data) => {
            alert(data.message)
            open_modal = false;
            files = {};
            loading = true;
            invalidate("/api/load-files");
        });
    }

    let deleteFileLocal = (filename) => {
        deleteFile(filename, delete_success_callback, delete_error_callback);
    }

    let open_modal_callback =
        (response) => {
            response.json().then((data) => {
                title = data.title;
                mimetype = data.mime_type;
                text = data.text;
                open_modal = true;
            });
        }

    let error_callback = (response) => {
        response.json().then((data) => {
            console.log(data.message);
        });
    }

    let fetch_file = (filename) => {
        loadFile(filename, open_modal_callback, error_callback);
    }

</script>

{#if !loading}
    <div class="lg:w-3/4 xl:w-1/2 sm:w-full m-auto py-3">
        <Table hoverable="true">
            <TableHead>
                <TableHeadCell>Filename</TableHeadCell>
                <TableHeadCell>Uploaded at</TableHeadCell>
                <TableHeadCell>Mime-Type</TableHeadCell>
                <TableHeadCell></TableHeadCell>
            </TableHead>
            <TableBody class="divide-y">
                {#if files === null || Object.keys(files).length === 0}
                    <TableBodyRow>
                        <TableBodyCell colspan="4" class="text-center">No files found</TableBodyCell>
                    </TableBodyRow>
                {:else}
                    {#each Object.entries(files) as [key, item]}
                        <TableBodyRow>
                            <TableBodyCell>{item.name}</TableBodyCell>
                            <TableBodyCell>{item.last_modified}</TableBodyCell>
                            <TableBodyCell>{item.mime_type}</TableBodyCell>
                            <TableBodyCell>
                                <ButtonSecondary on:click={() => {fetch_file(item.name)}}>
                                    <FolderOpenOutline/>
                                </ButtonSecondary>
                            </TableBodyCell>
                        </TableBodyRow>
                    {/each}
                {/if}
            </TableBody>
        </Table>
    </div>
{/if}

<div class="h-screen justify-center flex">
    <div class="m-auto w-fit justify-center flex">
        {#if loading}
            <p class="animate-bounce">Loading...</p>
        {/if}
    </div>
</div>

<Modal title="Index Preview" bind:open={open_modal} autoclose>
    <div class="p-5 overflow-scroll h-full">
        <div class="pb-3">
            <p class="text-sm text-gray-500 dark:text-gray-400">Title: {title}</p>
        </div>
        <div class="pb-3">
            <p class="text-sm text-gray-500 dark:text-gray-400">Mine-Type: {mimetype}</p>
        </div>
        <div class="pb-3">
            <p class="text-sm text-gray-500 dark:text-gray-400">Text-Preview:</p>
            <p class="text-sm text-gray-500 dark:text-gray-400 h-[426px] overflow-scroll border rounded-xl px-1 py-2 whitespace-pre-line">{text}</p>
        </div>
        <div class="block">
            <Button on:click={closeModal}>Close</Button>
            <ButtonSecondary on:click={() => {deleteFileLocal(title)}}>Delete</ButtonSecondary>
        </div>
    </div>
</Modal>
