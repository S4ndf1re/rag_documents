<!--
<script>
    import {browser} from "$app/environment";

    let navItems = [{name: 'Upload', page: "/upload"},
        {name: 'View Files', page: '/view'},
        {name: 'Query', page: '/query'},
        {name: 'Quiz', page: '/quiz'},
        {name: 'Contact', page: "/contact"}];

    let currently_selected = "";
    if (browser) {
        currently_selected = window.location.pathname;
    }

</script>

{@debug currently_selected}

<nav class="bg-secondary-200">
    <ul>
        {#each navItems as item}
            {#if currently_selected === item.page.toLowerCase()}
                <li class="hover:bg-secondary-400 w-full py-5 bg-secondary-300">
                    <a class="w-full h-full m-auto flex justify-center"
                       href={`${item.page.toLowerCase()}`}>{item.name}</a>
                </li>
            {:else}
                <li class="hover:bg-secondary-400 w-full py-5">
                    <a class="w-full h-full m-auto flex justify-center"
                       href={`${item.page.toLowerCase()}`}>{item.name}</a>
                </li>
            {/if}
        {/each}
    </ul>
</nav>

<style>
    ul {
        list-style: none;
        padding: 0;
        display: flex;
        justify-content: center;
    }

    li {
        /* Styling for list items */
    }

    a {
        text-decoration: none;
        color: black;
        /* Additional anchor tag styling */
    }

    /* Responsive design adjustments */
    @media (max-width: 600px) {
        ul {
            flex-direction: column;
        }
    }
</style>-->
