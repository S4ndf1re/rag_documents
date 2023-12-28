<script lang="ts">
    import {deleteFile, loadFile, loadFiles, queryChunks} from "$lib/api.js";
    import {
        Table,
        TableBody,
        TableBodyCell,
        TableBodyRow,
        TableHead,
        TableHeadCell,
        Button, Modal, Input, Card
    } from 'flowbite-svelte';
    import {FolderOpenOutline} from "flowbite-svelte-icons";
    import ButtonSecondary from "$lib/ButtonSecondary.svelte";
    import {load} from "./+page";
    import {invalidate, invalidateAll} from "$app/navigation";

    let open_modal = false;

    let data = [];

    let callback = (response: Response) => {
        if (response.status === 200) {
            response.json().then((json) => {
                data = json;
            });
        } else {
            response.json().then((json) => {
                alert("Error: " + json.message);
            });
        }
    }

    let error_callback = (error: Error) => {
        alert("Error: " + error.message);
    }

    let query = "";
    let on_keydown = (event: KeyboardEvent) => {
        if (event.key === "Enter") {
            queryChunks(query, callback, error_callback);
        }
    }

</script>

<div class="lg:w-3/4 xl:w-1/2 sm:w-full m-auto py-3">
    <div class="w-full py-3">
        <Input type="text" id="query" bind:value={query} on:keydown={on_keydown} placeholder="Query"/>
    </div>
    <div class="w-full">
        {#each data as chunk}
            <div class="my-3">
                <Card class="w-full max-w-full">
                    <p>{chunk.text}</p>
                    <div class="bg-gray-300 w-full h-[1px] mt-2"></div>
                    <div class="flex flex-row mt-2">
                        <div class="mr-2">
                            <p class="text-gray-400">Filename: {chunk.filename}</p>
                        </div>
                        <p class="text-gray-400">|</p>
                        <div class="ml-2">
                            <p class="text-gray-400">Index: {chunk.idx}</p>
                        </div>
                        <p class="text-gray-400">|</p>
                        <div class="ml-2">
                            <p class="text-gray-400">Score: {chunk.score}</p>
                        </div>
                    </div>
                </Card>
            </div>
        {/each}
    </div>
</div>

<Modal title="Index Preview" bind:open={open_modal} autoclose>
    <div class="p-5 overflow-scroll h-full">
    </div>
</Modal>
