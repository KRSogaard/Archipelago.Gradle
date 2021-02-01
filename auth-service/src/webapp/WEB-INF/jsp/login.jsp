<%@ include file="common/header.jspf"%>

    <font color="red">${response_type}</font>
    <form method="post">
        Name : <input type="text" name="name" />
        Password : <input type="password" name="password" />
        <input type="submit" />
    </form>

<%@ include file="common/footer.jspf"%>