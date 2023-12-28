<script>
    import {
        Table,
        TableBody,
        TableBodyCell,
        TableBodyRow,
        TableHead,
        TableHeadCell,
        Checkbox,
        TableSearch
    } from 'flowbite-svelte';

    /** @type {import('./$types').PageData} */
    export let data;

    let searchTerm = '';
    let filteredFiles

    $: files = data.data;

    $: if (searchTerm === ''){
        filteredFiles = files;
    } else {
        filteredFiles = files;
        filteredFiles.filter((file) => file.name.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1);
    }
</script>

<Table hoverable={true}>
    <TableSearch placeholder="Search by document name" hoverable={true} bind:inputValue={searchTerm}>
    <TableHead>
        <TableHeadCell class="!p-4">
            <Checkbox/>
        </TableHeadCell>
        <TableHeadCell>Filename</TableHeadCell>
        <TableHeadCell>Uploaded At</TableHeadCell>
        <TableHeadCell>Mime-Type</TableHeadCell>
        <TableHeadCell>
            <span class="sr-only">Edit</span>
        </TableHeadCell>
    </TableHead>
    <TableBody class="divide-y">
        {#each Object.entries(files) as [key, item]}
            <TableBodyRow>
                <TableBodyCell class="!p-4">
                    <Checkbox/>
                </TableBodyCell>
                <TableBodyCell>{item.name}</TableBodyCell>
                <TableBodyCell>{item.last_modified}</TableBodyCell>
                <TableBodyCell>{item.mime_type}</TableBodyCell>
                <TableBodyCell>
                    <a href="/tables"
                       class="font-medium text-primary-600 hover:underline dark:text-primary-500">Edit</a>
                </TableBodyCell>
            </TableBodyRow>
        {/each}
    </TableBody>
    </TableSearch>
</Table>